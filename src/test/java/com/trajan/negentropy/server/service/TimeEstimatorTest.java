//package com.trajan.negentropy.server.service;
//
//import com.trajan.negentropy.server.facade.TaskUpdateService;
//import com.trajan.negentropy.server.facade.response.NodeResponse;
//import com.trajan.negentropy.server.model.Task;
//import com.trajan.negentropy.server.model.TaskLink;
//import com.trajan.negentropy.server.repository.LinkRepository;
//import com.trajan.negentropy.server.repository.TaskRepository;
//import com.trajan.negentropy.server.repository.TimeEstimateRepository;
//import com.trajan.negentropy.server.task.TaskEntityQueryService;
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import org.hibernate.Session;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import java.sql.Statement;
//import java.time.Duration;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest
//@ActiveProfiles("test")
//public class TimeEstimatorTest {
//
//    @Autowired
//    private TaskUpdateService updateService;
//
//    @Autowired
//    private TaskEntityQueryService queryService;
//
//    @Autowired
//    private TimeEstimateRepository timeRepository;
//
//    @Autowired
//    private TaskRepository taskRepository;
//
//    @Autowired
//    private LinkRepository linkRepository;
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    private Task parentTask;
//    private Task childTask1, childTask2, childTask1_1, childTask1_2, childTask2_1, childTask2_2;
//
//    @BeforeEach
//    public void setUp() {
//        // Create and save tasks
//        parentTask = Task.builder()
//                .name("Parent task")
//                .description("A sample parent task")
//                .duration(Duration.ofHours(1))
//                .build();
//        // 1H
//        childTask1 = Task.builder()
//                .name("Child task 1")
//                .description("A sample child task 1")
//                .duration(Duration.ofHours(2))
//                .build();
//        // 3H
//        childTask2 = Task.builder()
//                .name("Child task 2")
//                .description("A sample child task 2")
//                .duration(Duration.ofHours(3))
//                .build();
//        // 6H
//        childTask1_1 = Task.builder()
//                .name("Child task 1.1")
//                .description("A sample child task 1.1")
//                .duration(Duration.ofHours(1))
//                .build();
//        // 7H
//        childTask1_2 = Task.builder()
//                .name("Child task 1.2")
//                .description("A sample child task 1.2")
//                .duration(Duration.ofHours(2))
//                .build();
//         // 9H
//        childTask2_1 = Task.builder()
//                .name("Child task 2.1")
//                .description("A sample child task 2.1")
//                .duration(Duration.ofHours(1))
//                .build();
//        // 10H
//        childTask2_2 = Task.builder()
//                .name("Child task 2.2")
//                .description("A sample child task 2.2")
//                .duration(Duration.ofHours(2))
//                .build();
//        // 12H
//
//        // Create task hierarchy
//        parentTask = updateService.addTaskAsRoot(parentTask).child();
//        childTask1 = updateService.addTaskAsChild(parentTask.id(), childTask1).child();
//        NodeResponse r2   = updateService.addTaskAsChild(parentTask.id(), childTask2);
//        childTask2 = r2.child();
//        NodeResponse r1_1 = updateService.addTaskAsChild(childTask1.id(), childTask1_1);
//        NodeResponse r1_2 = updateService.addTaskAsChild(childTask1.id(), childTask1_2);
//        NodeResponse r2_1 = updateService.addTaskAsChild(childTask2.id(), childTask2_1);
//        NodeResponse r2_2 = updateService.addTaskAsChild(childTask2.id(), childTask2_2);
//
//        parentTask = r2.parent();
//        childTask1 = r1_2.parent();
//        childTask2 = r2_2.parent();
//        childTask1_1 = r1_1.child();
//        childTask1_2 = r1_2.child();
//        childTask2_1 = r2_1.child();
//        childTask2_2 = r2_2.child();
//    }
//
//    @AfterEach
//    public void tearDown() {
//        Session session = entityManager.unwrap(Session.class);
//        session.doWork(connection -> {
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
//            }
//        });
//
//        timeRepository.deleteAll();
//        linkRepository.deleteAll();
//        taskRepository.deleteAll();
//
//        session.doWork(connection -> {
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
//            }
//        });
//    }
//
//    @Test
//    public void timeEstimatesCreatedForNewTasks() {
//        // Check if time estimates exist for all tasks
////        assertNotNull(timeRepository.findOneByTask(parentTask));
////        assertNotNull(timeRepository.findOneByTask(childTask1));
////        assertNotNull(timeRepository.findOneByTask(childTask2));
////        assertNotNull(timeRepository.findOneByTask(childTask1_1));
////        assertNotNull(timeRepository.findOneByTask(childTask1_2));
////        assertNotNull(timeRepository.findOneByTask(childTask2_1));
////        assertNotNull(timeRepository.findOneByTask(childTask2_2));
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        Duration childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        Duration childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(5), childTask1Estimate);
//        assertEquals(Duration.ofHours(6), childTask2Estimate);
//
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenChildTaskAdded() {
//        Task newChildTask = Task.builder()
//                .name("New Child task")
//                .description("A new child task")
//                .duration(Duration.ofHours(2))
//                .build();
//
//        updateService.addTaskAsChild(parentTask.id(), newChildTask);
//
//        // Check if time estimates have been updated correctly
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        assertEquals(Duration.ofHours(14), parentTaskEstimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenChildTaskRemoved() {
//        updateService.deleteTask(childTask1_1.id());
//
//        // Check if time estimates have been updated correctly
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        assertEquals(Duration.ofHours(11), parentTaskEstimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenChildTaskWithChildrenRemoved() {
//        updateService.deleteTask(childTask1.id());
//
//        // Check if time estimates have been updated correctly
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        assertEquals(Duration.ofHours(7), parentTaskEstimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenLinkDeleted() {
//
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenChildTaskDurationUpdated() {
//        // Update childTask1's duration, increase by 1
//        childTask1.duration(Duration.ofHours(3));
//        updateService.updateTask(childTask1);
//
//        // Check if time estimates have been updated correctly
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        Duration childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        assertEquals(Duration.ofHours(13), parentTaskEstimate);
//        assertEquals(Duration.ofHours(6), childTask1Estimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenTaskMovedToAnotherParent() {
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        Duration childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        Duration childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(5), childTask1Estimate);
//        assertEquals(Duration.ofHours(6), childTask2Estimate);
//
//        // Move childTask1_1 under childTask2
//        TaskLink oldLink = childTask1_1.parentLinks().get(0);
//        updateService.addTaskAsChild(childTask2.id(), childTask1_1);
//        updateService.deleteLink(oldLink.id());
//
//        // Check if time estimates have been updated correctly
//        parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(4), childTask1Estimate);
//        assertEquals(Duration.ofHours(7), childTask2Estimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenTaskDuplicatedToAnotherParent() {
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        Duration childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        Duration childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(5), childTask1Estimate);
//        assertEquals(Duration.ofHours(6), childTask2Estimate);
//
//        // Move childTask1_1 under childTask2
//        updateService.addTaskAsChild(childTask2.id(), childTask1_1);
//
//        // Check if time estimates have been updated correctly
//        parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(5), childTask1Estimate);
//        assertEquals(Duration.ofHours(7), childTask2Estimate);
//    }
//
//    @Test
//    public void timeEstimatesUpdatedWhenTaskDuplicatedToAnotherParentAndUpdated() {
//        Duration parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        Duration childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        Duration childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//
//        assertEquals(Duration.ofHours(12), parentTaskEstimate);
//        assertEquals(Duration.ofHours(5), childTask1Estimate);
//        assertEquals(Duration.ofHours(6), childTask2Estimate);
//
//        // Move childTask1_1 under childTask2
//        NodeResponse response = updateService.addTaskAsChild(childTask2.id(), childTask1_1);
//        childTask1_1 = response.child();
//        childTask1_1.duration(Duration.ofHours(2));
//        updateService.updateTask(childTask1_1);
//
//        // Check if time estimates have been updated correctly
//        parentTaskEstimate = queryService.getEstimatedTotalDuration(parentTask.id());
//        childTask1Estimate = queryService.getEstimatedTotalDuration(childTask1.id());
//        childTask2Estimate = queryService.getEstimatedTotalDuration(childTask2.id());
//        assertEquals(Duration.ofHours(14), parentTaskEstimate);
//        assertEquals(Duration.ofHours(6), childTask1Estimate);
//        assertEquals(Duration.ofHours(8), childTask2Estimate);
//    }
//}
