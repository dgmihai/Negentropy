package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Routine;
import com.trajan.negentropy.server.entity.RoutineStep;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.status.RoutineStatus;
import com.trajan.negentropy.server.entity.status.TaskStatus;
import com.trajan.negentropy.server.repository.RoutineRepository;
import com.trajan.negentropy.server.repository.RoutineStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutineServiceImpl implements RoutineService {
    private static final Logger logger = LoggerFactory.getLogger(RoutineServiceImpl.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private RoutineRepository routineRepository;
    @Autowired
    private RoutineStepRepository routineStepRepository;

    public RoutineServiceImpl(RoutineRepository routineRepository, RoutineStepRepository routineStepRepository, TaskService taskService) {
        this.routineRepository = routineRepository;
        this.routineStepRepository = routineStepRepository;
        this.taskService = taskService;
    }

    private void dfs(Task task, Routine routine, int priority) {
        for (TaskNode childNode : taskService.getChildNodes(task.getId())) {
            if (childNode.getPriority() >= priority) {
                Task childTask = childNode.getReferenceTask();
                if (taskService.countChildNodes(childTask.getId()) == 0) {
                    routine.getQueue().add(childTask);
                    routine.setEstimatedDuration(routine.getEstimatedDuration().plus(
                            childTask.getDuration()));
                }
                dfs(childTask, routine, priority + childNode.getPriority());
            }
        }
    }

    @Override
    @Transactional
    public Routine initRoutine(long taskId, int priority) {
        Task task = taskService.getTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        Routine routine = Routine.builder()
                .rootTask(task)
                .status(RoutineStatus.INITIALIZED)
                .priority(priority)
                .build();

        dfs(routine.getRootTask(), routine, routine.getPriority());
        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine recalculateRoutine(long routineId, int priority) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found with ID: + " + routineId));

        routine.setPriority(priority);
        routine.setQueue(new ArrayList<>());

        dfs(routine.getRootTask(), routine, routine.getPriority());
        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine startRoutine(long routineId) {
        Routine routine = routineRepository.findById(routineId)
                .orElseThrow(() -> new IllegalArgumentException("Routine not found with ID: + " + routineId));

        RoutineStep routineStep = RoutineStep.builder()
                .routine(routine)
                .task(routine.getQueue().get(0))
                .status(TaskStatus.ACTIVE)
                .start(LocalDateTime.now())
                .build();
        routine.getSteps().add(routineStep);
        routine.setStatus(RoutineStatus.ACTIVE);

        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine completeStep(long routineId) {
        Routine routine = getRoutine(routineId);
        int index = routine.getSteps().size();
        RoutineStep step = routine.getSteps().get(routine.getSteps().size() - 1);
        step.setStatus(TaskStatus.COMPLETED);
        step.setFinish(LocalDateTime.now());

        if (routine.getQueue().size() == routine.getSteps().size()) {
            routine.setStatus(RoutineStatus.COMPLETED);
        } else {
            RoutineStep nextStep = this.createRoutineStep(routine, routine.getQueue().get(index));
            nextStep.setStatus(TaskStatus.ACTIVE);
            routine.getSteps().add(nextStep);
        }
        routine.setEstimatedDuration(routine.getEstimatedDuration().minus(step.getTask().getDuration()));
        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine skipStep(long routineId) {
        Routine routine = this.getRoutine(routineId);
        int index = routine.getSteps().size();
        RoutineStep step = routine.getSteps().get(index - 1);
        step.setStatus(TaskStatus.SKIPPED);
        step.setFinish(LocalDateTime.now());

        if (routine.getQueue().size() == routine.getSteps().size()) {
            routine.setStatus(RoutineStatus.COMPLETED);
        } else {
            RoutineStep nextStep = this.createRoutineStep(routine, routine.getQueue().get(index));
            nextStep.setStatus(TaskStatus.ACTIVE);
            routine.getSteps().add(nextStep);
        }
        routine.setEstimatedDuration(routine.getEstimatedDuration().minus(step.getTask().getDuration()));
        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine suspendStep(long routineId) {
        Routine routine = this.getRoutine(routineId);
        RoutineStep step = routine.getSteps().get(routine.getSteps().size() - 1);
        step.setStatus(TaskStatus.SUSPENDED);

        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine resumeStep(long routineId) {
        Routine routine = this.getRoutine(routineId);
        RoutineStep step = routine.getSteps().get(routine.getSteps().size() - 1);
        step.setStatus(TaskStatus.ACTIVE);

        return routineRepository.save(routine);
    }

    @Override
    @Transactional
    public Routine setRoutineStepStatus(long routineStepId, TaskStatus status) {
        RoutineStep step = routineStepRepository.findById(routineStepId).orElseThrow();
        Routine routine = this.getRoutine(step.getRoutine().getId());
        step.setStatus(status);
        return routineRepository.save(routine);
    }

    // TODO: This is not correct
    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        for (RoutineStep step : routineStepRepository.findByStatus(TaskStatus.ACTIVE)) {
            step.setElapsedActiveTime(step.getElapsedActiveTime().plusSeconds(1));
        }
    }

    private RoutineStep createRoutineStep(Routine routine, Task task) {
        RoutineStep routineStep = RoutineStep.builder()
                .task(task)
                .routine(routine)
                .start(LocalDateTime.now())
                .build();
        routineStep.setRoutine(routine);
        return routineStep;
    }

    @Override
    public Routine getRoutine(long routineId) {
        return routineRepository.findById(routineId).orElseThrow();
    }

    @Override
    public List<Routine> findRoutinesByStatus(RoutineStatus status) {
        return routineRepository.findByStatus(status);
    }

    @Override
    public Boolean doesAnyRoutineStepReferToTask(long taskId) {
        return routineStepRepository.existsByTaskId(taskId);
    }
}
