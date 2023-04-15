package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskInfo_;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.TaskNode_;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;


//    Map<String, TaskInfo> taskInfos = new HashMap<>();
//    Map<String, TaskNode> taskNodes = new HashMap<>();



    @BeforeEach
    public void setUp() {
        TaskInfo taskInfo1 = new TaskInfo().toBuilder()
                .title("Task 1")
                .duration(Duration.ofMinutes(10))
                .priority(1)
                .build();
        taskService.saveTaskInfo(taskInfo1);

        TaskInfo taskInfo2 = new TaskInfo().toBuilder()
                .title("Task 2")
                .duration(Duration.ofMinutes(20))
                .priority(2)
                .build();
        taskService.saveTaskInfo(taskInfo2);

        TaskInfo taskInfo3 = new TaskInfo().toBuilder()
                .title("Task 3")
                .duration(Duration.ofMinutes(30))
                .priority(3)
                .build();
        taskService.saveTaskInfo(taskInfo3);

        TaskInfo rootTaskInfo = taskService.getRootTaskInfo();

        TaskNode taskNode1 = TaskNode.builder()
                .parent(rootTaskInfo)
                .child(taskInfo1)
                .next(null)
                .build();
        taskService.saveTaskNode(taskNode1);

        TaskNode taskNode2 = TaskNode.builder()
                .parent(rootTaskInfo)
                .child(taskInfo2)
                .next(null)
                .build();
        taskService.saveTaskNode(taskNode2);

        TaskNode taskNode3 = TaskNode.builder()
                .parent(rootTaskInfo)
                .child(taskInfo3)
                .next(null)
                .build();
        taskService.saveTaskNode(taskNode3);
    }

    @AfterEach
    void cleanUp() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "task_nodes", "task_info");
    }

    @Test
    public void testSaveTaskInfoWithDuplicateTitle() {
        List<TaskInfo> taskInfoList = taskService.findTaskInfos(Collections.emptyList());
        assertEquals(3, taskInfoList.size());

        String duplicateTitle = "Task 1";
        Duration duration = Duration.ofMinutes(10);
        int priority = 1;
        TaskInfo taskInfo1 = new TaskInfo().toBuilder()
                .title(duplicateTitle)
                .duration(duration)
                .priority(priority)
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> taskService.saveTaskInfo(taskInfo1));
    }

    @Test
    public void testGetTaskInfoById() {
        // Create a new TaskInfo object
        Duration duration = Duration.ofMinutes(10);
        int priority = 1;
        String title = "Task 4";
        String description = "Test task";
        TaskInfo taskInfo = new TaskInfo()
                .toBuilder()
                .duration(duration)
                .priority(priority)
                .title(title)
                .description(description)
                .build();

        // Save the TaskInfo object to the database
        taskService.saveTaskInfo(taskInfo);

        // Retrieve the id of the saved TaskInfo object
        Long taskId = taskInfo.getId();

        // Retrieve the TaskInfo object using the getTaskInfoById() method
        TaskInfo retrievedTaskInfo = taskService.getTaskInfoById(taskId);

        // Assert that the retrieved TaskInfo object is not null and has the correct id
        assertNotNull(retrievedTaskInfo);
        assertEquals(taskId, retrievedTaskInfo.getId());
    }

    @Test
    public void testFindTaskInfosWithMultipleFilters() {
        // Create a list of filters to apply
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("title")
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build());
        filters.add(Filter.builder()
                .field("duration")
                .operator(QueryOperator.LESS_THAN)
                .value(Duration.ofMinutes(20))
                .build());

        // Call the findTaskInfos method with the filters
        List<TaskInfo> taskInfos = taskService.findTaskInfos(filters);

        // Verify that the returned list contains the correct TaskInfo entities
        assertEquals(1, taskInfos.size());
        assertEquals("Task 1", taskInfos.get(0).getTitle());
        assertEquals(Duration.ofMinutes(10), taskInfos.get(0).getDuration());
    }

    @Test
    public void testFindTaskInfosWithEmptyFilters() {
        // Create an empty list of filters
        List<Filter> filters = new ArrayList<>();

        // Call the findTaskInfos method with the empty filters
        List<TaskInfo> taskInfos = taskService.findTaskInfos(filters);

        // Verify that the returned list contains all TaskInfo entities
        assertEquals(3, taskInfos.size());
    }

    @Test
    public void testFindTaskInfosWithInvalidField() {
        // Create a list of filters with an invalid field name
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("invalid_field")
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build());

        // Call the findTaskInfos method with the invalid filters
        assertThrows(InvalidDataAccessApiUsageException.class, () -> taskService.findTaskInfos(filters));
    }

    @Test
    public void testFindTaskInfosWithInvalidOperator() {
        // Create a list of filters with an invalid operator
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("title")
                .operator(QueryOperator.EMPTY)
                .build());

        // Call the findTaskInfos method with the invalid filters
        assertThrows(ClassCastException.class, () -> taskService.findTaskInfos(filters));
    }

    @Test
    public void testFindTaskInfosWithInvalidValue() {
        // Create a list of filters with an invalid value
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field("title")
                .operator(QueryOperator.EQUALS)
                .value(123)
                .build());

        // Call the findTaskInfos method with the invalid filters
        List<TaskInfo> taskInfos = taskService.findTaskInfos(filters);

        // Verify that the returned list is empty
        assertTrue(taskInfos.isEmpty());
    }

    @Test
    public void testDeleteNewTaskInfo() {
        // Create a new TaskInfo entity with a unique title
        TaskInfo taskInfoToDelete = new TaskInfo().toBuilder()
                .title("Task to Delete")
                .duration(Duration.ofMinutes(10))
                .priority(1)
                .build();
        taskService.saveTaskInfo(taskInfoToDelete);

        // Delete the TaskInfo entity
        taskService.deleteTaskInfo(taskInfoToDelete);

        // Verify that the TaskInfo entity was deleted correctly
        List<TaskInfo> result = taskService.findTaskInfos(Collections.singletonList(Filter.builder()
                .field("title")
                .operator(QueryOperator.EQUALS)
                .value("Task to Delete")
                .build()));
        assertTrue(result.isEmpty());
    }


    @Test
    public void testDeleteExistingTaskInfo() {
        TaskInfo one = taskService.findTaskInfos(Collections.singletonList(Filter.builder()
                .field(TaskInfo_.TITLE)
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build())).get(0);
        TaskNode oneNode = taskService.findTaskNodes(Collections.singletonList(Filter.builder()
                .field(TaskNode_.CHILD)
                .operator(QueryOperator.EQUALS)
                .value(one)
                .build())).get(0);

        assertNotNull(one);
        assertNotNull(oneNode);

        // Delete a new TaskInfo
        taskService.deleteTaskInfo(one);

//        // Clear the persistence context
//        entityManager.clear();

        // Verify that the new TaskInfo was deleted correctly
        List<TaskInfo> result = taskService.findTaskInfos(Collections.singletonList(Filter.builder()
                .field("title")
                .operator(QueryOperator.EQUALS)
                .value("Task 1")
                .build()));
        assertTrue(result.isEmpty());

        // Verify that the associated TaskNode was also deleted
        assertEquals(0, taskService.findTaskNodes(Collections.singletonList(Filter.builder()
                .field("parent")
                .operator(QueryOperator.EQUALS)
                .value(one)
                .build())).size());
    }

    @Test
    public void testDeleteTaskInfoRoot() {
        // Get the current _ROOT TaskInfo entity
        TaskInfo rootTaskInfo = taskService.getRootTaskInfo();

        // Verify that the _ROOT TaskInfo entity exists before deletion
        assertNotNull(rootTaskInfo);

        // Attempt to delete the _ROOT TaskInfo entity
        assertThrows(IllegalArgumentException.class, () -> taskService.deleteTaskInfo(rootTaskInfo));

        // Verify that the _ROOT TaskInfo entity still exists after the attempted deletion
        TaskInfo rootTaskInfoAfter = taskService.getRootTaskInfo();
        assertEquals(rootTaskInfo, rootTaskInfoAfter);
    }


    @Test
    public void testSaveTaskNode() {
        // Test that saveTaskNode() correctly saves a TaskNode entity
    }

    @Test
    public void testGetTaskNodeById() {
        // Create a new TaskInfo and save it
        TaskInfo taskInfo = new TaskInfo().toBuilder()
                .title("Task 4")
                .duration(Duration.ofMinutes(15))
                .priority(2)
                .build();
        taskService.saveTaskInfo(taskInfo);

        // Create a new TaskNode with the TaskInfo as the child
        TaskInfo rootTaskInfo = taskService.getRootTaskInfo();
        TaskNode taskNode = TaskNode.builder()
                .parent(rootTaskInfo)
                .child(taskInfo)
                .next(null)
                .build();
        taskService.saveTaskNode(taskNode);

        // Retrieve the TaskNode by ID and verify that it's the correct one
        TaskNode retrievedTaskNode = taskService.getTaskNodeById(taskNode.getId());
        assertNotNull(retrievedTaskNode);
        assertEquals(taskNode.getId(), retrievedTaskNode.getId());
        assertEquals(rootTaskInfo.getId(), retrievedTaskNode.getParent().getId());
        assertEquals(taskInfo.getId(), retrievedTaskNode.getChild().getId());
        assertNull(retrievedTaskNode.getNext());
    }


    @Test
    public void testFindTaskNodes() {
        TaskInfo rootTaskInfo = taskService.getRootTaskInfo();
        // Create a list of filters to apply
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.builder()
                .field(TaskNode_.PARENT)
                .operator(QueryOperator.EQUALS)
                .value(taskService.getRootTaskInfo())
                .build());

        // Call the findTaskNodes method with the filters
        List<TaskNode> taskNodes = taskService.findTaskNodes(filters);

        // Verify that the returned list contains the correct TaskNode entities
        assertEquals(3, taskNodes.size());
        assertEquals(rootTaskInfo.getId(), taskNodes.get(0).getParent().getId());
        assertNull(taskNodes.get(0).getNext());
    }


    @Test
    public void testDeleteTaskNode() {
        // Test that deleteTaskNode() correctly deletes a TaskNode entity
    }

    @Test
    public void testSaveTaskNodeWithInvalidParent() {
        // Test that saveTaskNode() throws an IllegalArgumentException when the parent TaskInfo entity does not exist
    }

    @Test
    public void testSaveTaskNodeWithInvalidPrevOrNext() {
        // Test that saveTaskNode() throws an IllegalArgumentException when the prev or next TaskNode entity does not share the same parent TaskInfo entity
    }
}

