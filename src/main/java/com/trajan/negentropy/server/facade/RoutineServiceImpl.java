//package com.trajan.negentropy.server.service;
//
//import com.trajan.negentropy.server.entity.Routine;
//import com.trajan.negentropy.server.entity.RoutineStep;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.status.RoutineStatus;
//import com.trajan.negentropy.server.entity.status.TaskStatus;
//import com.trajan.negentropy.server.facade.RoutineService;
//import com.trajan.negentropy.server.repository.RoutineRepository;
//import com.trajan.negentropy.server.repository.RoutineStepRepository;
//import com.trajan.negentropy.server.task.TaskQueryService;
//import com.trajan.negentropy.server.task.TaskUpdateService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class RoutineServiceImpl implements RoutineService {
//    private static final Logger logger = LoggerFactory.getLogger(RoutineServiceImpl.class);
//
//    @Autowired
//    private TaskQueryService queryService;
//    @Autowired
//    private TaskUpdateService updateService;
//
//    @Autowired
//    private RoutineRepository routineRepository;
//    @Autowired
//    private RoutineStepRepository routineStepRepository;
//
////    private void dfs(Task task, Routine routine) {
////        for (TaskLink childNode : taskService.getChildNodes(task.getId())) {
////            Task childTask = childNode.getReferenceTask();
////            if (taskService.countChildNodes(childTask.getId()) == 0) {
////                routine.getQueue().add(childTask);
////            }
////            dfs(childTask, routine);
////        }
////    }
//
//    @Override
//    @Transactional
//    public Routine startNewRoutine(long taskId) {
//        Task task = queryService.getTask(taskId).orElseThrow(() ->
//                new IllegalArgumentException("Task not found with ID: " + taskId));
//
//        Routine routine = Routine.builder()
//                .rootTask(task)
//                .status(RoutineStatus.ACTIVE)
////                .estimatedDuration(queryService.getTimeEstimate(taskId))
//                .build();
//
////        dfs(routine.getRootTask(), routine);
//
//        List<RoutineStep> steps = queryService.getAllDescendantNodes(task)
//                .map(link -> RoutineStep.builder()
//                            .routine(routine)
//                            .task(link.referenceTask())
//                            .status(TaskStatus.NOT_STARTED)
//                            .build())
//                .toList();
//
//        RoutineStep firstStep = RoutineStep.builder()
//                .routine(routine)
//                .task(task)
//                .status(TaskStatus.ACTIVE)
//                .start(LocalDateTime.now())
//                .lastResumed(LocalDateTime.now())
//                .build()
//
//        if (!queryService.hasParents(task)) {
//            routine.steps(List.of());
//        }
//
//        RoutineStep routineStep = RoutineStep.builder()
//                .routine(routine)
//                .task(routine.getQueue().get(0))
//                .status(TaskStatus.ACTIVE)
//                .start(LocalDateTime.now())
//                .lastResumed(LocalDateTime.now())
//                .build();
//
//        routine.getSteps().add(routineStep);
//
//        return routineRepository.save(routine);
//    }
//
//    @Override
//    @Transactional
//    public Routine recalculateRoutine(long routineId, int importance) {
//        Routine routine = routineRepository.findById(routineId)
//                .orElseThrow(() -> new IllegalArgumentException("Routine not found with ID: + " + routineId));
//
//        routine.setPriority(importance);
//        routine.setQueue(new ArrayList<>());
//
//        dfs(routine.getRootTask(), routine);
//        return routineRepository.save(routine);
//    }
//
//    @Override
//    @Transactional
//    public Routine completeStep(long routineId) {
//        return routineRepository.save(this.nextStep(routineId,
//                TaskStatus.COMPLETED));
//    }
//
//    @Override
//    @Transactional
//    public Routine skipStep(long routineId) {
//        return routineRepository.save(this.nextStep(routineId,
//                TaskStatus.SKIPPED));
//    }
//
//    private Routine nextStep(long routineId, TaskStatus prevStatus) {
//        Routine routine = this.getRoutine(routineId);
//        List<RoutineStep> steps = routine.getSteps();
//        List<Task> queue = routine.getQueue();
//        RoutineStep step = steps.get(steps.size() - 1);
//        step.setFinish(LocalDateTime.now());
//        step.setStatus(prevStatus);
//        if (queue.size() == steps.size()) {
//            routine.setStatus(RoutineStatus.COMPLETED);
//        } else {
//            RoutineStep nextStep = this.createRoutineStep(routine, queue.get(steps.size()));
//            nextStep.setStatus(TaskStatus.ACTIVE);
//            steps.add(nextStep);
//        }
//        routine.setEstimatedDuration(routine.getEstimatedDuration().minus(step.getTask().getDuration()));
//        return routine;
//    }
//    @Override
//    @Transactional
//    public Routine suspendStep(long routineId) {
//        Routine routine = this.getRoutine(routineId);
//        List<RoutineStep> steps = routine.getSteps();
//        RoutineStep step = steps.get(steps.size() - 1);
//        if (step.getStatus() == TaskStatus.ACTIVE) {
//            Duration duration = Duration.between(step.getLastResumed(), LocalDateTime.now());
//            step.setElapsedActiveTime(step.getElapsedActiveTime().plus(duration));
//        }
//        step.setStatus(TaskStatus.SUSPENDED);
//
//        return routineRepository.save(routine);
//    }
//
//    @Override
//    @Transactional
//    public Routine resumeStep(long routineId) {
//        Routine routine = this.getRoutine(routineId);
//        List<RoutineStep> steps = routine.getSteps();
//        RoutineStep step = steps.get(steps.size() - 1);
//        if (step.getStatus() != TaskStatus.ACTIVE) {
//            step.setLastResumed(LocalDateTime.now());
//        }
//        step.setStatus(TaskStatus.ACTIVE);
//
//        return routineRepository.save(routine);
//    }
//
//    @Override
//    @Transactional
//    public Routine setRoutineStepStatus(long routineStepId, TaskStatus status) {
//        RoutineStep step = routineStepRepository.findById(routineStepId).orElseThrow();
//        Routine routine = this.getRoutine(step.getRoutine().getId());
//        step.setStatus(status);
//        return routineRepository.save(routine);
//    }
//
//    private RoutineStep createRoutineStep(Routine routine, Task task) {
//        RoutineStep routineStep = RoutineStep.builder()
//                .task(task)
//                .routine(routine)
//                .start(LocalDateTime.now())
//                .lastResumed(LocalDateTime.now())
//                .build();
//        routineStep.setRoutine(routine);
//        return routineStep;
//    }
//
//    @Override
//    public Routine getRoutine(long routineId) {
//        return routineRepository.findById(routineId).orElseThrow();
//    }
//
//    @Override
//    public List<Routine> findRoutinesByStatus(RoutineStatus status) {
//        return routineRepository.findByStatus(status);
//    }
//
//    @Override
//    public Boolean doesAnyRoutineStepReferToTask(long taskId) {
//        return routineStepRepository.existsByTaskId(taskId);
//    }
//
//    @Override
//    public RoutineStep getCurrentStep(long routineId) {
//        List<RoutineStep> steps = this.getRoutine(routineId).getSteps();
//        return steps.get(steps.size() - 1);
//    }
//}
