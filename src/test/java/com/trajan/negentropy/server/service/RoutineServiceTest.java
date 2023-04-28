//package com.trajan.negentropy.server.service;
//
//import com.trajan.negentropy.server.entity.Routine;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.TaskLink;
//import com.trajan.negentropy.server.entity.Task_;
//import com.trajan.negentropy.server.entity.status.RoutineStatus;
//import com.trajan.negentropy.server.repository.RoutineRepository;
//import com.trajan.negentropy.server.repository.RoutineStepRepository;
//import com.trajan.negentropy.server.repository.LinkRepository;
//import com.trajan.negentropy.server.repository.TaskRepository;
//import com.trajan.negentropy.server.repository.filter.Filter;
//import com.trajan.negentropy.server.repository.filter.QueryOperator;
//import exclude.TaskService;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.hibernate.Session;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.util.Pair;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.sql.Statement;
//import java.time.Duration;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@ExtendWith(SpringExtension.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@SpringBootTest
//public class RoutineServiceTest {
//    @Autowired
//    private TaskService taskService;
//    @Autowired
//    private RoutineService routineService;
//    @Autowired
//    private TaskRepository taskRepository;
//    @Autowired
//    private LinkRepository taskNodeRepository;
//    @Autowired
//    private RoutineRepository routineRepository;
//    @Autowired
//    private RoutineStepRepository stepRepository;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @BeforeEach
//    public void setUp() {
//        Task task0 = Task.builder()
//                .name("Root")
//                .duration(Duration.ofMinutes(5))
//                .build();
//        Task task1 = Task.builder()
//                .name("Task 1")
//                .duration(Duration.ofMinutes(1))
//                .build();
//        Task task2 = Task.builder()
//                .name("Task 2")
//                .duration(Duration.ofMinutes(2))
//                .build();
//        Task task3 = Task.builder()
//                .name("Task 3")
//                .duration(Duration.ofMinutes(3))
//                .build();
//
//        Pair<Task, TaskLink> result0 = taskService.createTaskWithNode(task0);
//        task0 = result0.getFirst();
//        TaskLink node0 = result0.getSecond();
//
//        task1 = taskService.createTask(task1);
//        task2 = taskService.createTask(task2);
//        task3 = taskService.createTask(task3);
//
//        taskService.createChildNode(task0.getId(), task1.getId(), 0);
//        taskService.createChildNode(task0.getId(), task2.getId(), 1);
//        taskService.createChildNode(task0.getId(), task3.getId(), 2);
//    }
//
//    @AfterEach
//    @Transactional
//    public void tearDown() {
//        Session session = entityManager.unwrap(Session.class);
//        session.doWork(connection -> {
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
//            }
//        });
//
//        taskNodeRepository.deleteAll();
//        taskRepository.deleteAll();
//
//
//        session.doWork(connection -> {
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
//            }
//        });
//    }
//
//    @Test
//    public void testInitRoutine() {
//        // Starting a routine with task0 as the root
//        Filter filter = Filter.builder()
//                .field(Task_.TITLE)
//                .operator(QueryOperator.EQUALS)
//                .value("Root")
//                .build();
//        Routine routine = routineService.startNewRoutine(taskService.findTasks(List.of(filter)).get(0).getId());
//
//        // Check if routine is not null and not completed
//        assertNotNull(routine);
//        assertEquals(RoutineStatus.ACTIVE, routine.getStatus());
//
//        // Check that the routine queue is properly populated
//        assertEquals("Task 1", routine.getQueue().get(0).getTitle());
//        assertEquals("Task 2", routine.getQueue().get(1).getTitle());
//        assertEquals("Task 3", routine.getQueue().get(2).getTitle());
//
//        // Check if the queue contains Task 2 and Task 3
//        List<String> queueTaskTitles = routine.getQueue().stream()
//                .map(Task::getTitle)
//                .toList();
//        assertEquals(3, queueTaskTitles.size());
//        assertTrue(queueTaskTitles.contains("Task 1"));
//        assertTrue(queueTaskTitles.contains("Task 2"));
//        assertTrue(queueTaskTitles.contains("Task 3"));
//
//        assertEquals(Duration.ofMinutes(6), routine.getEstimatedDuration());
//    }
//
//    @Test
//    @Disabled
//    public void testInitRoutineWithPriority() {
//        // Starting a routine with task0 as the root
//        Filter filter = Filter.builder()
//                .field(Task_.TITLE)
//                .operator(QueryOperator.EQUALS)
//                .value("Root")
//                .build();
//        Routine routine = routineService.startNewRoutine(taskService.findTasks(List.of(filter)).get(0).getId());
//
//        // Check if routine is not null and not completed
//        assertNotNull(routine);
//        assertEquals(RoutineStatus.INITIALIZED, routine.getStatus());
//
//        // Check that the routine queue is properly populated
//        assertEquals("Task 3", routine.getQueue().get(0).getTitle());
//
//        // Check if the queue contains Task 2 and Task 3
//        List<String> queueTaskTitles = routine.getQueue().stream()
//                .map(Task::getTitle)
//                .toList();
//        assertEquals(1, queueTaskTitles.size());
//        assertFalse(queueTaskTitles.contains("Task 1"));
//        assertFalse(queueTaskTitles.contains("Task 2"));
//        assertTrue(queueTaskTitles.contains("Task 3"));
//
//        assertEquals(Duration.ofMinutes(3), routine.getEstimatedDuration());
//    }
//
//    @Test
//    public void testIterateThroughRoutine() {
//        // Starting a routine with task0 as the root
//        Filter filter = Filter.builder()
//                .field(Task_.TITLE)
//                .operator(QueryOperator.EQUALS)
//                .value("Root")
//                .build();
//        Routine routine = routineService.startNewRoutine(taskService.findTasks(List.of(filter)).get(0).getId());
//
//        // Check if the routine is active
//        assertEquals(RoutineStatus.ACTIVE, routine.getStatus());
//
//        // Check if the current step is Task 1
//        assertEquals("Task 1", routine.getQueue().get(0).getTitle());
//        assertEquals("Task 1", routine.getSteps().get(0).getTask().getTitle());
//        assertEquals("Task 1", routine.getCurrentStep().getTask().getTitle());
//
//        // Complete the first step (Task 1) and start the next step (Task 2)
//        routine = routineService.completeStep(routine.getId());
//
//        // Verify the estimated duration has updated
//        assertEquals(Duration.ofMinutes(5), routine.getEstimatedDuration());
//
//        // Check if the current step is Task 2
//        assertEquals("Task 2", routine.getQueue().get(1).getTitle());
//        assertEquals("Task 2", routine.getSteps().get(1).getTask().getTitle());
//        assertEquals("Task 2", routine.getCurrentStep().getTask().getTitle());
//
//        // Skip the second step (Task 2)
//        routine = routineService.skipStep(routine.getId());
//
//        // Verify the estimated duration has updated
//        assertEquals(Duration.ofMinutes(3), routine.getEstimatedDuration());
//
//        // Check if the current step is Task 3
//        assertEquals("Task 3", routine.getQueue().get(2).getTitle());
//        assertEquals("Task 3", routine.getSteps().get(2).getTask().getTitle());
//        assertEquals("Task 3", routine.getCurrentStep().getTask().getTitle());
//
//        // Complete the third step (Task 3)
//        routine = routineService.completeStep(routine.getId());
//
//        // Verify the estimated duration has updated
//        assertEquals(Duration.ZERO, routine.getEstimatedDuration());
//
//        // Check if the routine is completed
//        assertEquals(RoutineStatus.COMPLETED, routine.getStatus());
//    }
//}
