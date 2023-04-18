package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.TaskSession;
import com.trajan.negentropy.server.entity.TaskStatus;
import com.trajan.negentropy.server.repository.TaskSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringBootTest
public class RoutineServiceTest {

    @Autowired
    private TaskSessionRepository taskSessionRepository;

    @Autowired
    private TaskService taskService;

    private RoutineService routineService;

    @BeforeEach
    public void setUp() {
        routineService = new RoutineServiceImpl(taskSessionRepository, taskService);
    }

    @AfterEach
    public void tearDown() {
        TaskSession activeTaskSession = routineService.getActiveTaskSession();
        if (activeTaskSession != null) {
            routineService.pauseTask(activeTaskSession.getId());
        }
    }

    @Test
    public void testStartTask() {
        Task task = Task.builder().title("Task 1").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = routineService.startTask(node.getId());

        assertNotNull(taskSession);
        assertEquals(TaskStatus.ACTIVE, taskSession.getStatus());
        assertNotNull(taskSession.getStartTime());
        assertNull(taskSession.getPauseTime());
        assertEquals(Duration.ZERO, taskSession.getTotalPausedDuration());
    }

    @Test
    public void testStartTaskWhenAnotherTaskIsActive() {
        Task task1 = Task.builder().title("Task 101").build();
        task1 = taskService.createTaskWithNode(task1).getFirst();
        Pair<Task, TaskNode> result1 = taskService.createTaskWithNode(task1);
        task1 = result1.getFirst();
        TaskNode node1 = result1.getSecond();

        Task task2 = Task.builder().title("Task 102").build();
        Pair<Task, TaskNode> result2 = taskService.createTaskWithNode(task2);
        task2 = result2.getFirst();
        TaskNode node2 = result2.getSecond();

        long task2Id = task2.getId();
        assertThrows(IllegalStateException.class, () -> routineService.startTask(task2Id));

        TaskSession taskSession1 = taskSessionRepository.findByNodeId(task1.getId()).orElse(null);
        assertNotNull(taskSession1);
        assertEquals(TaskStatus.ACTIVE, taskSession1.getStatus());

        TaskSession taskSession2 = taskSessionRepository.findByNodeId(task2.getId()).orElse(null);
        assertNull(taskSession2);
    }

    @Test
    public void testPauseTask() {
        Task task = Task.builder().title("Task 2").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = routineService.startTask(node.getId());
        routineService.pauseTask(taskSession.getId());

        taskSession = taskSessionRepository.findByNodeId(taskSession.getId()).orElse(null);
        assertNotNull(taskSession);
        assertEquals(TaskStatus.PAUSED, taskSession.getStatus());
        assertNotNull(taskSession.getPauseTime());
    }

    @Test
    public void testResumeTask() {
        Task task = Task.builder().title("Task 3").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = routineService.startTask(node.getId());
        routineService.pauseTask(taskSession.getId());
        routineService.resumeTask(taskSession.getId());

        taskSession = taskSessionRepository.findByNodeId(node.getId()).orElse(null);
        assertNotNull(taskSession);
        assertEquals(TaskStatus.ACTIVE, taskSession.getStatus());
        assertNull(taskSession.getPauseTime());
    }

    @Test
    public void testResumeTaskWhenAnotherTaskIsActive() {
        Task task1 = Task.builder().title("Task 103").build();
        task1 = taskService.createTask(task1);
        routineService.startTask(task1.getId());
        routineService.pauseTask(task1.getId());

        Task task2 = Task.builder().title("Task 104").build();
        task2 = taskService.createTask(task2);
        routineService.startTask(task2.getId());

        long task1Id = task1.getId();
        assertThrows(IllegalStateException.class, () -> routineService.resumeTask(task1Id));

        TaskSession taskSession1 = taskSessionRepository.findByNodeId(task1.getId()).orElse(null);
        assertNotNull(taskSession1);
        assertEquals(TaskStatus.PAUSED, taskSession1.getStatus());

        TaskSession taskSession2 = taskSessionRepository.findByNodeId(task2.getId()).orElse(null);
        assertNotNull(taskSession2);
        assertEquals(TaskStatus.ACTIVE, taskSession2.getStatus());
    }

    @Test
    public void testCompleteTask() {
        Task task = Task.builder().title("Task 4").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        routineService.startTask(node.getId());
        routineService.completeTask(task.getId());

        TaskSession taskSession = taskSessionRepository.findByNodeId(task.getId()).orElse(null);
        assertNotNull(taskSession);
        assertEquals(TaskStatus.COMPLETED, taskSession.getStatus());
    }

    @Test
    public void testPauseNotRunningTask() {
        Task task = Task.builder().title("Task 5").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = new TaskSession(node, TaskStatus.PAUSED, LocalDateTime.now(), null, Duration.ZERO);
        taskSessionRepository.save(taskSession);

        routineService.pauseTask(task.getId());

        TaskSession updatedTaskSession = taskSessionRepository.findByNodeId(task.getId()).orElse(null);
        assertNotNull(updatedTaskSession);
        assertEquals(TaskStatus.PAUSED, updatedTaskSession.getStatus());
    }

    @Test
    public void testResumeNotPausedTask() {
        Task task = Task.builder().title("Task 6").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = new TaskSession(node, TaskStatus.ACTIVE, LocalDateTime.now(), null, Duration.ZERO);
        taskSession = taskSessionRepository.save(taskSession);

        long sessionId = taskSession.getId();
        assertThrows(IllegalStateException.class, () -> routineService.resumeTask(sessionId));

        TaskSession updatedTaskSession = taskSessionRepository.findByNodeId(node.getId()).orElse(null);
        assertNotNull(updatedTaskSession);
        assertEquals(TaskStatus.ACTIVE, updatedTaskSession.getStatus());
    }

    @Test
    public void testCompleteNotRunningTask() {
        Task task = Task.builder().title("Task 7").build();
        Pair<Task, TaskNode> result = taskService.createTaskWithNode(task);
        task = result.getFirst();
        TaskNode node = result.getSecond();

        TaskSession taskSession = new TaskSession(node, TaskStatus.PAUSED, LocalDateTime.now(), null, Duration.ZERO);
        taskSessionRepository.save(taskSession);

        routineService.completeTask(task.getId());

        TaskSession updatedTaskSession = taskSessionRepository.findByNodeId(task.getId()).orElse(null);
        assertNotNull(updatedTaskSession);
        assertEquals(TaskStatus.COMPLETED, updatedTaskSession.getStatus());
    }

    @Test
    public void testPauseNotInRepository() {
        Task task = Task.builder().title("Task 8").build();
        task = taskService.createTask(task);

        long taskId = task.getId();
        assertThrows(IllegalArgumentException.class, () -> routineService.pauseTask(taskId));
    }

    @Test
    public void testResumeNotInRepository() {
        Task task = Task.builder().title("Task 9").build();
        task = taskService.createTask(task);

        long taskId = task.getId();
        assertThrows(IllegalArgumentException.class, () -> routineService.pauseTask(taskId));
    }

    @Test
    public void testCompleteNotInRepository() {
        Task task = Task.builder().title("Task 10").build();
        task = taskService.createTask(task);

        long taskId = task.getId();
        assertThrows(IllegalArgumentException.class, () -> routineService.pauseTask(taskId));
    }

}
