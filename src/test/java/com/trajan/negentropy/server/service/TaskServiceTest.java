package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.Task_;
import com.trajan.negentropy.server.repository.TaskNodeRepository;
import com.trajan.negentropy.server.repository.TaskRepository;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

import java.sql.Statement;
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
    private TaskRepository taskRepository;
    @Autowired
    private TaskNodeRepository taskNodeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TaskService taskService;

    ArrayList<Task> tasks = new ArrayList<>(
            Collections.nCopies(4, null));
    ArrayList<TaskNode> nodes = new ArrayList<>(
            Collections.nCopies(4, null));

    @BeforeEach
    @Transactional
    public void setUp() {
        assertEquals(0, taskService.findTasks(
                Collections.emptyList()).size());
        assertEquals(0, taskService.findAllNodes(
                Collections.emptyList()).size());

        Task task0 = Task.builder()
                .title("Root")
                .build();
        Task task1 = Task.builder()
                .title("Task 1")
                .duration(Duration.ofMinutes(10))
                .build();
        Task task2 = Task.builder()
                .title("Task 2")
                .duration(Duration.ofMinutes(20))
                .build();
        Task task3 = Task.builder()
                .title("Task 3")
                .duration(Duration.ofMinutes(30))
                .build();
        Pair<Task, TaskNode> resultPair = taskService.createTaskWithNode(task0);
        tasks.set(0, resultPair.getFirst());
        nodes.set(0, resultPair.getSecond());
        tasks.set(1, taskService.createTask(task1));
        tasks.set(2, taskService.createTask(task2));
        tasks.set(3, taskService.createTask(task3));

        nodes.set(1, taskService.createChildNode(
                tasks.get(0).getId(),
                tasks.get(1).getId(),
                1));
        nodes.set(3, taskService.createNodeAfter(
                tasks.get(3).getId(),
                nodes.get(1).getId(),
                3));

        assertEquals(nodes.get(1), nodes.get(3).getPrev());
        assertNull(nodes.get(1).getPrev());

        nodes.set(1, taskService.getNode(nodes.get(1).getId()).orElseThrow());

        assertNull(nodes.get(3).getNext());
        assertEquals(nodes.get(3), nodes.get(1).getNext());

        nodes.set(2, taskService.createNodeBefore(
                tasks.get(2).getId(),
                nodes.get(3).getId(),
                2));

        nodes.set(1, taskService.getNode(nodes.get(1).getId()).orElseThrow());
        nodes.set(3, taskService.getNode(nodes.get(3).getId()).orElseThrow());

        List<TaskNode> children = taskService.getChildNodes(
                tasks.get(0).getId());

        assertEquals(3, children.size());
        assertEquals(nodes.get(1), children.get(0));
        assertEquals(nodes.get(2), children.get(1));
        assertEquals(nodes.get(3), children.get(2));

        assertEquals(nodes.get(1), nodes.get(2).getPrev());
        assertEquals(nodes.get(2), nodes.get(3).getPrev());
        assertNull(nodes.get(1).getPrev());

        assertNull(nodes.get(3).getNext());
        assertEquals(nodes.get(2), nodes.get(1).getNext());
        assertEquals(nodes.get(3), nodes.get(2).getNext());

        assertEquals(4, taskService.findTasks(
                Collections.emptyList()).size());
        assertEquals(4, taskService.findAllNodes(
                Collections.emptyList()).size());
    }

    @AfterEach
    @Transactional
    public void tearDown() {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            }
        });

        taskNodeRepository.deleteAll();
        taskRepository.deleteAll();

        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        });
    }

    @Test
    public void testSaveTaskWithDuplicateTitle() {
        List<Task> taskList = taskService.findTasks(Collections.emptyList());
        assertEquals(4, taskList.size());

        Task taskDuplicate = Task.builder()
                .title("Task 1")
                .duration(Duration.ofMinutes(11))
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
                .field(Task_.DURATION)
                .operator(QueryOperator.SHORTER_THAN)
                .value(Duration.ofDays(1))
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
        // TODO: Tag filter
//        filters.add(Filter.builder()
//                .field(Task_.PRIORITY)
//                .operator(QueryOperator.GREATER_THAN)
//                .value(0)
//                .build());

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
        // TODO: Tag filter
//        filters.add(Filter.builder()
//                .field(Task_.PRIORITY)
//                .operator(QueryOperator.LESS_THAN)
//                .value(3)
//                .build());

        // Call the findTasks method with the filters
        results = taskService.findTasks(filters);

        assertEquals(3, results.size());
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
    public void testDeleteTaskAndTaskNode() {
        Task task4 = Task.builder()
                .title("Task 4")
                .duration(Duration.ofMinutes(40))
                .build();

        Task task4_1 = Task.builder()
                .title("Task 4_1")
                .duration(Duration.ofMinutes(41))
                .build();
        Task task4_2 = Task.builder()
                .title("Task 4_2")
                .duration(Duration.ofMinutes(42))
                .build();
        Task task4_3 = Task.builder()
                .title("Task 4_3")
                .duration(Duration.ofMinutes(43))
                .build();
        task4 = taskService.createTask(task4);
        task4_1 = taskService.createTask(task4_1);
        task4_2 = taskService.createTask(task4_2);
        task4_3 = taskService.createTask(task4_3);

        TaskNode taskNode4_1 = taskService.createChildNode(task4.getId(), task4_1.getId(), 1);
        TaskNode taskNode4_3 = taskService.createChildNode(task4.getId(), task4_3.getId(), 3);
        TaskNode taskNode4_2= taskService.createNodeBefore(task4_2.getId(), taskNode4_3.getId(), 2);

        // Verify that nodes added properly

        // Call the findNodes to fine all children of Task4, unordered
        List<TaskNode> results = taskService.getChildNodes(task4.getId());
        assertEquals(3, results.size());

        // Delete a taskNode, expecting the linked list to still be properly formed
        taskService.deleteNode(taskNode4_2.getId());
        results = taskService.getChildNodes(task4.getId());
        assertEquals(2, results.size());
        assertEquals(taskNode4_3, taskService.getNode(taskNode4_1.getId()).orElseThrow().getNext());
        assertEquals(taskNode4_1, taskService.getNode(taskNode4_3.getId()).orElseThrow().getPrev());

        // Delete a Task and expect all child tasks to be gone
        taskService.deleteTask(task4.getId());
        final long task4Id = task4.getId();
        assertThrows(NoSuchElementException.class, () -> taskService.getChildNodes(task4Id));
        assertEquals(1, taskService.getOrphanNodes().size());
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
        List<TaskNode> results = taskService.getChildNodes(tasks.get(0).getId());

        // Verify that the returned list contains the correct TaskNode entities and is ordered
        assertEquals(3, results.size());
        assertNull(results.get(2).getNext());
        assertNull(results.get(0).getPrev());
    }

    @Test
    public void testSaveInvalidTaskNode() {
        // Assert that saving a node with a bad parent ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.createChildNode(-1, tasks.get(1).getId(), 0));

        // Assert that saving a node with a bad next node ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.createNodeBefore(tasks.get(1).getId(), -1, 0));

        // Assert that saving a node with a bad task data ID throws
        assertThrows(NoSuchElementException.class, () -> taskService.createChildNode(tasks.get(1).getId(), -1, 0));
        assertThrows(NoSuchElementException.class, () -> taskService.createNodeBefore(-1, tasks.get(1).getId(), 0));
    }

    @Test
    public void testCreateChildNodeAfterChildNode() {
        Task parentTask = Task.builder()
                .title("Parent task")
                .description("Description of parent task")
                .build();

        Task childTask1 = Task.builder()
                .title("Child Task 1")
                .description("Description of Child Task 1")
                .build();

        Task childTask2 = Task.builder()
                .title("Child Task 2")
                .description("Description of Child Task 2")
                .build();

        Pair<Task, TaskNode> parent = taskService.createTaskWithNode(parentTask);
        childTask1 = taskService.createTask(childTask1);
        childTask2 = taskService.createTask(childTask2);
        TaskNode childNode1 = taskService.createChildNode(parent.getFirst().getId(), childTask1.getId(), 0);
        TaskNode childNode2 = taskService.createNodeAfter(childTask2.getId(), childNode1.getId(), 0);
        childNode1 = taskService.getNode(childNode1.getId()).orElseThrow();

        assertEquals(2, taskService.getChildNodes(parent.getFirst().getId()).size());
        assertEquals(childNode2, childNode1.getNext());
        assertEquals(childNode1, childNode2.getPrev());
    }
}

