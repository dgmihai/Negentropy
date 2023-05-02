package com.trajan.negentropy.server.task;

import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.facade.TaskQueryService;
import com.trajan.negentropy.server.facade.TaskUpdateService;
import com.trajan.negentropy.server.facade.model.*;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;

import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TaskUpdateServiceTest {

    @Autowired private TaskUpdateService updateService;
    @Autowired private TaskEntityQueryService entityQueryService;
    @Autowired private TaskQueryService queryService;

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;

    @Autowired private EntityMapper mapper;

    @PersistenceContext private EntityManager entityManager;

    private Map<String, Task> tasks = new HashMap<>();
    private final Map<String, TaskEntity> taskEntities = new HashMap<>();
    private final Map<String, TaskLink> links = new HashMap<>();

    @BeforeEach
    void setUp() {
        List<String> taskNames = List.of(
                "1",
                "2",
                "3",
                "4",
                    "21",
                    "22",
                    "23",
                        "221",
                        "222");

        // Populate test data
        Pair<Map<String, Task>, List<Task>> result = taskNames.stream()
                .map(name -> new Task(null)
                        .name(name))
                .map(task -> updateService.createTask(task).task())
                .peek(task -> assertNotNull(task.id()))
                .collect(Collectors.teeing(
                        Collectors.toMap(
                                Task::name,
                                task -> task),
                        Collectors.toList(),
                        Pair::of
                ));

        tasks = result.getFirst();
        List <Task> taskList = result.getSecond();

        for(int i=0; i<4; i++) {
            updateService.insertTaskAsRoot(taskList.get(i).id());
        }

        TaskID task2Id = tasks.get("2").id();
        for(int i=4; i<7; i++) {
            updateService.insertTaskAsChild(task2Id, taskList.get(i).id());
        }

        TaskID task22Id = tasks.get("22").id();
        for(int i=7; i<9; i++) {
            updateService.insertTaskAsChild(task22Id, taskList.get(i).id());
        }

        for(Task task : tasks.values()) {
            taskEntities.put(task.name(), entityQueryService.getTask(task.id().val()));
        }
    }

    @AfterEach
    void tearDown() {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
            }
        });

        linkRepository.deleteAll();
        taskRepository.deleteAll();

        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        });
    }
    
    private void validateNodes(List<TaskNode> nodes, List<Task> tasks) {
        TaskID parentId = nodes.get(0).parentId();
        for (int i=0; i<nodes.size(); i++) {
            TaskNode node = nodes.get(i);
            Task task = tasks.get(i);
            if (parentId != null) {
                assertEquals(node.position(), i);
            }
            assertEquals(task.id(), node.childId());
            assertEquals(parentId, node.parentId());
        }
    }

    @Test
    void testTaskLinkOrder() {        
        List<TaskNode> rootNodes = queryService.getRootNodes();
        List<Task> rootTasks = List.of(
                tasks.get("1"),
                tasks.get("2"),
                tasks.get("3"),
                tasks.get("4"));
        validateNodes(rootNodes, rootTasks);
        
        List<TaskNode> task2ChildNodes = queryService.getChildNodes(tasks.get("2").id());
        List<Task> task2ChildTasks = List.of(
                tasks.get("21"), 
                tasks.get("22"), 
                tasks.get("23"));
        validateNodes(task2ChildNodes, task2ChildTasks);

        List<TaskNode> task22ChildNodes = queryService.getChildNodes(tasks.get("22").id());
        List<Task> task22ChildTasks = List.of(
                tasks.get("221"), 
                tasks.get("222"));
        validateNodes(task22ChildNodes, task22ChildTasks);
    }

    @Test
    void testSaveTaskAsChild() {
        Task fresh = new Task(null).name("Fresh");
        Task parent = tasks.get("2");

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskAsChild(parent.id(), fresh.id());
        assertTrue(response.success());
        
        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.getTask(response.node().childId());
        List<Task> expectedChildTasks = List.of(
                tasks.get("21"),
                tasks.get("22"),
                tasks.get("23"),
                fresh);
        validateNodes(
                queryService.getChildNodes(parent.id()),
                expectedChildTasks);
    }

    @Test
    void testAddTaskAsChildAt() {
        Task fresh = new Task(null).name("Fresh");
        Task parent = tasks.get("2");

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskAsChildAt(1, parent.id(), fresh.id());
        assertTrue(response.success());
        
        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.getTask(response.node().childId());
        List<Task> expectedChildTasks = List.of(
                tasks.get("21"),
                fresh,
                tasks.get("22"),
                tasks.get("23"));
        validateNodes(
                queryService.getChildNodes(parent.id()),
                expectedChildTasks);
    }

    @Test
    void testSaveTaskAsRoot() {
        Task f = new Task(null).name("Fresh");

        Task fresh = updateService.createTask(f).task();

        NodeResponse response = updateService.insertTaskAsRoot(fresh.id());

        assertNull(response.node().parentId());
        
        assertTrue(queryService.getRootNodes().stream()
                .anyMatch(node -> 
                        node.childId().equals(fresh.id())));
    }

    @Test
    void testUpdateTask() {
        Task task = tasks.get("1");
        task.name("Updated");

        TaskResponse response = updateService.updateTask(task);
        assertTrue(response.success());
        
        Task updatedTask = response.task();
        assertNotNull(updatedTask);

        assertEquals("Updated", updatedTask.name());
        task = queryService.getTask(tasks.get("1").id());
        assertEquals("Updated", task.name());
    }

    @Test
    @Disabled
    void testDeleteTask() {
        Task task = tasks.get("2");
        
        Response response = updateService.deleteTask(task.id());
        assertTrue(response.success());

        TaskEntity deletedTask = taskRepository.findById(task.id().val()).orElse(null);
        assertNull(deletedTask);
    }

    @Test
    void testDeleteLink() {
        Task task2 = tasks.get("2");
        Task task21 = tasks.get("21");
        TaskLink link = taskEntities.get("21").parentLinks().get(0);
        
        Response response = updateService.deleteNode(new LinkID(link.id()));
        assertTrue(response.success());

        assertThrows(IllegalArgumentException.class, () -> entityQueryService.getLink(link.id()));
        
        List<Task> expectedChildTasks = List.of(
                tasks.get("22"),
                tasks.get("23"));
        validateNodes(
                queryService.getChildNodes(task2.id()),
                expectedChildTasks);
        
        //TODO: Orphaned task cleanup
    }

    @Test
    public void testMoveTaskLink() {
        Task task1 = tasks.get("1");
        Task task2 = tasks.get("2");
        Task task21 = tasks.get("21");

        TaskLink taskLink3 = taskEntities.get("21").parentLinks().get(0);
        NodeResponse nodeResponse = updateService.insertTaskAsChild(task1.id(), task21.id());
        Response response = updateService.deleteNode(new LinkID(taskLink3.id()));

        assertTrue(nodeResponse.success());
        assertTrue(response.success());
        
        assertEquals(task1.id(), nodeResponse.node().parentId());

        List<Task> expectedTask1ChildTasks = List.of(
                tasks.get("21"));
        validateNodes(
                queryService.getChildNodes(task1.id()),
                expectedTask1ChildTasks);
        
        List<Task> expectedTask2ChildTasks = List.of(
                tasks.get("22"),
                tasks.get("23"));
        validateNodes(
                queryService.getChildNodes(task2.id()),
                expectedTask2ChildTasks);
    }

    @Test
    public void testTaskLinkOrderAfterChange() {
        Task task4 = tasks.get("4");
        Task task21 = tasks.get("21");
        Task task22 = tasks.get("22");
        Task task3 = tasks.get("3");

        NodeResponse r21 = updateService.insertTaskAsChild(task4.id(), task21.id());
        NodeResponse r22 = updateService.insertTaskAsChild(task4.id(), task22.id());
        NodeResponse r3 = updateService.insertTaskAsChildAt(r22.node().position(), task4.id(), task3.id());

        assertTrue(r21.success());
        assertTrue(r22.success());
        assertTrue(r3.success());
        
        List<Task> expectedChildren = List.of(
                tasks.get("21"),
                tasks.get("3"),
                tasks.get("22"));
        validateNodes(
                queryService.getChildNodes(task4.id()),
                expectedChildren);
    }

    @Test
    public void testCyclicalConnection() {
        Task task2 = tasks.get("2");
        Task task222 = tasks.get("222");

        NodeResponse response = updateService.insertTaskAsChild(task222.id(), task2.id());
        assertFalse(response.success());
    }

    @Test
    public void testAddToSelf() {
        Task task2 = tasks.get("2");

        NodeResponse response = updateService.insertTaskAsChild(task2.id(), task2.id());
        assertFalse(response.success());
    }

}