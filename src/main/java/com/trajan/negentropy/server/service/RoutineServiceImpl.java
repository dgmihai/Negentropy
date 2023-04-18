package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.TaskSession;
import com.trajan.negentropy.server.entity.TaskStatus;
import com.trajan.negentropy.server.repository.TaskSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RoutineServiceImpl implements RoutineService {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskSessionRepository taskSessionRepository;

    public RoutineServiceImpl(TaskSessionRepository taskSessionRepository, TaskService taskService) {
        this.taskSessionRepository = taskSessionRepository;
        this.taskService = taskService;
    }

    @Override
    public TaskSession startTask(long nodeId) {
        TaskSession activeTaskSession = taskSessionRepository.findActiveTaskSession();
        if (activeTaskSession != null) {
            throw new IllegalStateException("There is already an active task.");
        }

        TaskNode node = taskService.getNode(nodeId).orElseThrow();
        TaskSession taskSession = TaskSession.builder()
                .node(node)
                .status(TaskStatus.ACTIVE)
                .startTime(LocalDateTime.now())
                .build();
        return taskSessionRepository.save(taskSession);
    }

    @Override
    public void pauseTask(long sessionId) {
        TaskSession taskSession = getTaskSession(sessionId);
        taskSession.setStatus(TaskStatus.PAUSED);
        taskSession.setPauseTime(LocalDateTime.now());
        taskSessionRepository.save(taskSession);
    }

    @Override
    public void resumeTask(long sessionId) {
        TaskSession activeTaskSession = taskSessionRepository.findActiveTaskSession();
        if (activeTaskSession != null) {
            throw new IllegalStateException("There is already an active task.");
        }

        TaskSession taskSession = getTaskSession(sessionId);
        if (taskSession.getStatus() == TaskStatus.PAUSED) {
            taskSession.setStatus(TaskStatus.ACTIVE);
            Duration pauseDuration = Duration.between(taskSession.getPauseTime(), LocalDateTime.now());
            taskSession.setTotalPausedDuration(taskSession.getTotalPausedDuration().plus(pauseDuration));
            taskSession.setPauseTime(null);
        }
        taskSessionRepository.save(taskSession);
    }

    @Override
    public void completeTask(long sessionId) {
        TaskSession taskSession = getTaskSession(sessionId);
        taskSession.setStatus(TaskStatus.COMPLETED);
        taskSessionRepository.save(taskSession);
    }

    @Override
    public TaskSession getTaskSession(long sessionId) {
        return taskSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Task session not found"));
    }

    @Override
    public TaskSession getActiveTaskSession() {
        return taskSessionRepository.findActiveTaskSession();
    }
}
