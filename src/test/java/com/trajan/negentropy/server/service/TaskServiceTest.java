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
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    ArrayList<Task> tasks = new ArrayList<>(
            Collections.nCopies(4, null));
    ArrayList<TaskNode> nodes = new ArrayList<>(
            Collections.nCopies(4, null));

    @BeforeAll
    @Transactional
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
        Pair<Task, TaskNode> resultPair = taskService.createTaskWithNode(task0);
        tasks.set(0, resultPair.getFirst());
        nodes.set(0, resultPair.getSecond());
        tasks.set(1, taskService.createTask(task1));
        tasks.set(2, taskService.createTask(task2));
        tasks.set(3, taskService.createTask(task3));

        nodes.set(1, taskService.insertNodeAsChildOf(
                tasks.get(1).getId(),
                tasks.get(0).getId()));
        nodes.set(3, taskService.insertNodeAfter(
                tasks.get(3).getId(),
                nodes.get(1).getId()));
        nodes.set(2, taskService.insertNodeBefore(
                tasks.get(2).getId(),
                nodes.get(3).getId()));

        List<TaskNode> children = taskService.findChildNodes(
                tasks.get(0).getId());

        assertEquals(3, children.size());
        assertEquals(nodes.get(1), children.get(0));
        assertEquals(nodes.get(2), children.get(1));
        assertEquals(nodes.get(3), children.get(2));
        assertEquals(4, taskService.findTasks(
                Collections.emptyList()).size());
        assertEquals(4, taskService.findAllNodes(
                Collections.emptyList()).size());
    }

    @Test
    public void testSaveTaskWithDuplicateTitle() {
        List<Task> taskList = taskService.findTasks(Collections.emptyList());
        assertEquals(4, taskList.size());

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
        List<Task> results = taskService.findTasks(filters);

        // Verify that the returned list contains the correct Task entities
        assertEquals(0, results.size());

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

        // Fetch tasks with the new set of filters
        results = taskService.findTasks(filters);

        assertEquals("Task 3", results.get(0).getTitle());
        assertEquals(Duration.ofMinutes(30), results.get(0).getDuration());
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

    @Test
    @Transactional
    public void testUpdateTask() {
        // Get the task to update
        Task two = tasks.get(2);
        two.setTitle("Task 2 Updated");
        two.setDescription("This task has been updated.");

        two = taskService.updateTask(two);

        // See if the relevant references have been updated
        assertEquals("Task 2 Updated", tasks.get(2).getTitle());
        assertEquals(tasks.get(2), two);
        assertEquals(tasks.get(2).getTitle(), two.getTitle());

        // Run a search for the other updated description field
        List<Task> results = taskService.findTasks(List.of(Filter.builder()
                .field(Task_.DESCRIPTION)
                .operator(QueryOperator.LIKE)
                .value("updated")
                .build()));

        // See if we retrieve the correct task
        assertEquals(results.get(0).getTitle(), two.getTitle());
    }

    @Test
    @Transactional
    public void testDeleteTaskAndTaskNode() {
        // Add two nodes to anthe existing node, making a total of three nodes

        Task task4 = Task.builder()
                .title("Task 4")
                .duration(Duration.ofMinutes(40))
                .priority(4)
                .build();

        Task task4_1 = Task.builder()
                .title("Task 4_1")
                .duration(Duration.ofMinutes(41))
                .priority(1)
                .build();
        Task task4_2 = Task.builder()
                .title("Task 4_2")
                .duration(Duration.ofMinutes(42))
                .priority(2)
                .build();
        Task task4_3 = Task.builder()
                .title("Task 4_3")
                .duration(Duration.ofMinutes(43))
                .priority(3)
                .build();
        task4 = taskService.createTask(task4);
        task4_1 = taskService.createTask(task4_1);
        task4_2 = taskService.createTask(task4_2);
        task4_3 = taskService.createTask(task4_3);

        TaskNode taskNode4_1 = taskService.insertNodeAsChildOf(task4_1.getId(), task4.getId());
        TaskNode taskNode4_3 = taskService.insertNodeAsChildOf(task4_3.getId(), task4.getId());
        TaskNode taskNode4_2= taskService.insertNodeBefore(task4_2.getId(), taskNode4_3.getId());

        // Verify that nodes added properly

        // Call the findNodes to fine all children of Task4, unordered
        List<TaskNode> results = taskService.findChildNodes(task4.getId());
        assertEquals(3, results.size());

        // Delete a taskNode, expecting the linked list to still be properly formed
        taskService.deleteNode(taskNode4_2.getId());
        results = taskService.findChildNodes(task4.getId());
        assertEquals(2, results.size());
        assertEquals(taskNode4_3, taskNode4_1.getNext());
        assertEquals(taskNode4_1, taskNode4_3.getPrev());

        // Delete a Task and expect all child tasks to be gone
        taskService.deleteTask(task4.getId());
        final long task4Id = task4.getId();
        assertThrows(NoSuchElementException.class, () -> taskService.findChildNodes(task4Id));
        assertEquals(1, taskService.findOrphanNodes().size());
        assertEquals(4, taskService.findAllNodes(Collections.emptyList()).size());
        assertEquals(4, taskService.findTasks(Collections.emptyList()).size());
    }

    @Test
    public void testGetUnsavedTaskNode() {
        // Create a new TaskNode but don't save it
        TaskNode taskNode = TaskNode.builder()
                .parentTask(null)
                .referenceTask(null)
                .next(null)
                .build();

        // Retrieve the TaskNode by null ID and make sure it fails with exception
        assertThrows(NullPointerException.class, () -> taskService.getNode(taskNode.getId()));
    }

    @Test
    public void testFindChildNodes() {
        List<TaskNode> results = taskService.findChildNodes(tasks.get(0).getId());

        // Verify that the returned list contains the correct TaskNode entities and is ordered
        assertEquals(3, results.size());
        assertNull(results.get(2).getNext());
        assertNull(results.get(0).getPrev());
    }

    @Test
    public void testSaveInvalidTaskNode() {
        // Assert that saving a node with a bad parent ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.insertNodeAsChildOf(tasks.get(1).getId(), -1));

        // Assert that saving a node with a bad next node ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.insertNodeBefore(tasks.get(1).getId(), -1));

        // Assert that saving a node with a bad task data ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.insertNodeAsChildOf(-1, tasks.get(1).getId()));
        assertThrows(NoSuchElementException.class, () -> taskService.insertNodeBefore(-1, tasks.get(1).getId()));
    }
}

