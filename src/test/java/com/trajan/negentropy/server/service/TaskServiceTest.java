package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.Task_;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;
//    @Autowired
//    private JdbcTemplate jdbcTemplate;

    ArrayList<Task> tasks = new ArrayList<>(
            Collections.nCopies(4, null));
    ArrayList<TaskNode> nodes = new ArrayList<>(
            Collections.nCopies(4, null));

    @BeforeAll
    public void setUp() {
        Task task0 = Task.builder()
                .title("Root")
                .build();
        Task task1 = Task.builder()
                .title("Task 1")
                .duration(Duration.ofMinutes(10))
                .priority(1)
                .build();
        Task task2 = Task.builder()
                .title("Task 2")
                .duration(Duration.ofMinutes(20))
                .priority(2)
                .build();
        Task task3 = Task.builder()
                .title("Task 3")
                .duration(Duration.ofMinutes(30))
                .priority(3)
                .build();
        tasks.set(0, taskService.createTask(task0));
        tasks.set(1, taskService.createTask(task1));
        tasks.set(2, taskService.createTask(task2));
        tasks.set(3, taskService.createTask(task3));

        nodes.set(1, taskService.appendNodeTo(
                tasks.get(1).getId(),
                tasks.get(0).getId()));
        nodes.set(3, taskService.appendNodeTo(
                tasks.get(3).getId(),
                tasks.get(0).getId()));
        nodes.set(2, taskService.insertNodeBefore(
                tasks.get(2).getId(),
                nodes.get(3).getId()));

        assertEquals(3, taskService.getChildNodes(
                tasks.get(0).getId()).size());
        assertEquals(4, taskService.findTasks(
                Collections.emptyList()).size());
    }

//    @AfterEach
//    @Transactional
//    void cleanUp() {
//        JdbcTestUtils.deleteFromTables(jdbcTemplate, "task_nodes", "task_info");
//    }

    @Test
    public void testSaveTaskWithDuplicateTitle() {
        List<Task> taskList = taskService.findTasks(Collections.emptyList());
        assertEquals(3, taskList.size());

        Task taskDuplicate = Task.builder()
                .title("Task 1")
                .duration(Duration.ofMinutes(11))
                .priority(2)
                .build();
        assertThrows(DataIntegrityViolationException.class,
                () -> taskService.createTask(taskDuplicate));
    }

    @Test
    public void testGetTaskById() {
        // Retrieve the id of the saved Task object
        Long taskId1 = tasks.get(1).getId();

        // Retrieve the Task object using the getTaskInfoById() method
        Task task1 = taskService.getTask(taskId1).orElseThrow();

        // Assert that the retrieved Task object is not null and has the correct id
        assertNotNull(task1);
        assertEquals(taskId1, task1.getId());
        assertEquals(task1, tasks.get(1));
    }

    @Test
    public void testFindSingleTaskWithMultipleFilters() {
        // Create a list of filters to apply
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field(Task_.TITLE)
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build());
        filters.add(Filter.builder()
                .field(Task_.DURATION)
                .operator(QueryOperator.LONGER_THAN)
                .value(Duration.ofMinutes(20))
                .build());

        // Call the findTasks method with the filters
        List<Task> tasks = taskService.findTasks(filters);

        // Verify that the returned list contains the correct Task entities
        assertEquals(0, tasks.size());

        // Cycle through again
        filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field(Task_.TITLE)
                .operator(QueryOperator.EQUALS)
                .value("Task 3")
                .build());
        filters.add(Filter.builder()
                .field(Task_.PRIORITY)
                .operator(QueryOperator.GREATER_THAN)
                .value(2)
                .build());

        assertEquals("Task 3", tasks.get(0).getTitle());
        assertEquals(Duration.ofMinutes(30), tasks.get(0).getDuration());
    }

    @Test
    public void testFindMultipleTasksWithMultipleFilters() {
        // Create a list of filters to apply
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field(Task_.DURATION)
                .operator(QueryOperator.SHORTER_THAN)
                .value(Duration.ofMinutes(21))
                .build());
        filters.add(Filter.builder()
                .field(Task_.PRIORITY)
                .operator(QueryOperator.GREATER_THAN)
                .value(0)
                .build());

        // Call the findTasks method with the filters
        List<Task> results = taskService.findTasks(filters);

        // Verify that the returned list contains the correct Task entities
        assertEquals(2, results.size());
        assertTrue(results.contains(tasks.get(2)));
        assertTrue(results.contains(tasks.get(1)));

        filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field(Task_.TITLE)
                .operator(QueryOperator.LIKE)
                .value("Task")
                .build());
        filters.add(Filter.builder()
                .field(Task_.PRIORITY)
                .operator(QueryOperator.LESS_THAN)
                .value(3)
                .build());

        // Call the findTasks method with the filters
        results = taskService.findTasks(filters);

        assertEquals(2, results.size());
        assertTrue(tasks.contains(tasks.get(1)));
        assertTrue(tasks.contains(tasks.get(2)));
    }

    @Test
    public void testFindTasksWithEmptyFilters() {
        // Create an empty list of filters
        List<Filter> filters = List.of();

        // Call the findTaskInfos method with the empty filters
        List<Task> tasks = taskService.findTasks(filters);

        // Verify that the returned list contains all Task entities
        assertEquals(4, tasks.size());
    }

    @Test
    public void testFindTasksWithInvalidField() {
        // Create a list of filters with an invalid field name
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("invalid_field")
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build());

        // Call the findTaskInfos method with the invalid filters
        assertThrows(InvalidDataAccessApiUsageException.class, () -> taskService.findTasks(filters));
    }

    @Test
    public void testFindTasksWithInvalidOperator() {
        // Create a list of filters with an invalid operator
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("title")
                .operator(QueryOperator.EMPTY)
                .build());

        // Call the findTaskInfos method with the invalid filters
        assertThrows(ClassCastException.class, () -> taskService.findTasks(filters));
    }

    @Test
    public void testFindTasksWithInvalidValue() {
        // Create a list of filters with an invalid value
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("title")
                .operator(QueryOperator.EQUALS)
                .value(123)
                .build());

        // Call the findTaskInfos method with the invalid filters
        List<Task> tasks = taskService.findTasks(filters);

        // Verify that the returned list is empty
        assertTrue(tasks.isEmpty());
    }

//    @Test
//    public void testDeleteTaskInfoRoot() {
//        // Get the current Root TaskNode entity
//        TaskNode rootNode = taskService.getRootNode();
//
//        // Verify that the Root TaskNode entity exists before deletion
//        assertNotNull(rootNode);
//
//        // Attempt to delete the Root TaskNode entity
//        assertThrows(IllegalArgumentException.class, () -> taskService.deleteNode(rootNode.getId()));
//
//        // Verify that the _ROOT Task entity still exists after the attempted deletion
//        TaskNode rootTaskAfter = taskService.getRootNode();
//        assertEquals(rootNode, rootTaskAfter);
//    }

    @Test
    public void testSaveTaskNodes() {
//        // Add two nodes to the existing node, making a total of three nodes
//        Task task1 = taskService.findTasks(Collections.singletonList(Filter.builder()
//                .field(TaskInfo_.TITLE)
//                .operator(QueryOperator.EQUALS)
//                .value("Task 1")
//                .build())).get(0);
//
//        Task task4 = new Task().toBuilder()
//                .title("Task 4")
//                .duration(Duration.ofMinutes(40))
//                .priority(4)
//                .build();
//        task4 = taskService.createTask(task4);
//        TaskNode taskNode4 = TaskNode.builder()
//                .parent(task1)
//                .data(task4)
//                .next(null)
//                .build();
//        taskNode4 = taskService.saveTaskNode(taskNode4);
//
//        Task task6 = new Task().toBuilder()
//                .title("Task 6")
//                .duration(Duration.ofMinutes(60))
//                .priority(6)
//                .build();
//        task6 = taskService.createTask(task6);
//        TaskNode taskNode6 = TaskNode.builder()
//                .parent(task1)
//                .data(task6)
//                .next(null)
//                .build();
//        taskNode6 = taskService.saveTaskNode(taskNode6);
//
//        Task task5 = new Task().toBuilder()
//                .title("Task 5")
//                .duration(Duration.ofMinutes(50))
//                .priority(5)
//                .build();
//        task5 = taskService.createTask(task5);
//        TaskNode taskNode5 = TaskNode.builder()
//                .parent(task1)
//                .data(task5)
//                .next(taskNode6)
//                .build();
//        taskNode5 = taskService.saveTaskNode(taskNode5);
//
//        // Verify that each associated Task's getChildRootNode() method correctly returns the head of the linkedlist
//
//        assertEquals(taskNode4, taskService.getChildNodes(task1).get(0));
//
////        assertEquals(taskNode4, taskNode5.getPrev());
//        assertEquals(taskNode5, taskNode4.getNext());
////        assertEquals(taskNode5, taskNode6.getPrev());
//        assertEquals(taskNode6, taskNode5.getNext());
//        assertNull(taskNode6.getNext());
////        assertNull(taskNode4.getPrev());
    }



    @Test
    public void testGetTaskNodeByNullId() {
//        // Create a new TaskNode but don't save it
//        Task rootTask = taskService.getRootNode();
//        TaskNode taskNode = TaskNode.builder()
//                .parent(rootTask)
//                .data(null)
//                .next(null)
//                .build();
//
//        // Retrieve the TaskNode by null ID and make sure it fails with exception
//        assertThrows(IllegalArgumentException.class, () -> taskService.getNode(taskNode.getId()));
    }

    @Test
    public void testGetTaskNodeById() {
//        // Create a new Task and save it
//        Task task = new Task().toBuilder()
//                .title("Task 4")
//                .duration(Duration.ofMinutes(15))
//                .priority(2)
//                .build();
//        task = taskService.createTask(task);
//
//        // Create a new TaskNode with the Task as the child
//        Task rootTask = taskService.getRootNode();
//        TaskNode taskNode = TaskNode.builder()
//                .parent(rootTask)
//                .data(task)
//                .next(null)
//                .build();
//        taskNode = taskService.saveTaskNode(taskNode);
//
//        // Retrieve the TaskNode by ID and verify that it's the correct one
//        TaskNode retrievedTaskNode = taskService.getNode(taskNode.getId());
//        assertNotNull(retrievedTaskNode);
//        assertEquals(taskNode.getId(), retrievedTaskNode.getId());
//        assertEquals(rootTask.getId(), retrievedTaskNode.getParent().getId());
//        assertEquals(task.getId(), retrievedTaskNode.getData().getId());
//        assertNull(retrievedTaskNode.getNext());
    }

    @Test
    public void testFindTaskNodes() {
//        Task rootTask = taskService.getRootNode();
//        // Create a list of filters to apply
//        List<Filter> filters = new ArrayList<>();
//        filters.add(Filter.builder()
//                .field(TaskNode_.PARENT)
//                .operator(QueryOperator.EQUALS)
//                .value(taskService.getRootNode())
//                .build());
//
//        // Call the findTaskNodes method with the filters
//        List<TaskNode> taskNodes = taskService.findNodes(filters);
//
//        // Verify that the returned list contains the correct TaskNode entities
//        assertEquals(3, taskNodes.size());
//        assertEquals(rootTask.getId(), taskNodes.get(0).getParent().getId());
////        assertNull(taskNodes.get(0).getPrev());
//        assertNull(taskNodes.get(2).getNext());
    }


    @Test
    public void testDeleteTaskNode() {
        // Test that deleteTaskNode() correctly deletes a TaskNode entity
    }

    @Test
    public void testDeleteTask() {
//        // Create a new Task entity with a unique title
//        TaskData taskDataToDelete = TaskData.builder()
//                .title("Task to Delete")
//                .duration(Duration.ofMinutes(10))
//                .priority(1)
//                .build();
//        Task taskToDelete = taskService.createTask(taskDataToDelete);
//        TaskNodeData nodeDataToDelete = TaskNodeData.builder()
//                .parentNodeId(nodes.get(1).getId())
//                .dataTaskId(tasks.get(3).getId())
//                .nextNodeId(null)
//                .build();
//        nodes.set(3, taskService.insertNode(nodeData3));
//        // Delete the Task entity
//        taskService.deleteTask(taskToDelete.getId());
//
//        // Verify that the Task entity was deleted correctly
//        List<Task> result = taskService.findTasks(Collections.singletonList(Filter.builder()
//                .field(Task_.TITLE)
//                .operator(QueryOperator.EQUALS)
//                .value("Task to Delete")
//                .build()));
//        assertTrue(result.isEmpty());
    }

    @Test
    public void testSaveTaskNodeWithInvalidParent() {
        // Test that saveTaskNode() throws an IllegalArgumentException when the parent Task entity does not exist
    }

    @Test
    public void testSaveTaskNodeWithInvalidPrevOrNext() {
        // Test that saveTaskNode() throws an IllegalArgumentException when the prev or next TaskNode entity does not share the same parent Task entity
    }
}

