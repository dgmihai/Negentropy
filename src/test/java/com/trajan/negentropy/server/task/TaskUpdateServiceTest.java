package com.trajan.negentropy.server.task;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.facade.TaskQueryService;
import com.trajan.negentropy.server.facade.TaskUpdateService;
import com.trajan.negentropy.server.facade.response.LinkResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskLink;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TaskUpdateServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateServiceTest.class);

    @Autowired private TaskUpdateService updateService;
    @Autowired private TaskEntityQueryService entityQueryService;
    @Autowired private TaskQueryService queryService;

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;

    @Autowired private EntityMapper mapper;

    @PersistenceContext private EntityManager entityManager;

    private final Map<String, Task> tasks = new HashMap<>();
    private final Map<String, TaskLink> links = new HashMap<>();

    @BeforeEach
    void setUp() {
        List<String> taskNames = List.of(
                "One",
                "Two",
                "Three",
                "Four",
                    "TwoOne",
                    "TwoTwo",
                    "TwoThree");

        // Populate test data
        List<Task> taskList = taskNames.stream()
                .map(name -> Task.builder()
                        .name(name)
                        .build())
                .toList();

        for(int i=0; i<4; i++) {
            logger.debug(i + ": " + taskList.get(i).name());
            tasks.put(taskList.get(i).name(),
                    updateService.addTaskAsRoot(taskList.get(i)).child());
        }

        long taskTwoId = tasks.get("Two").id();
        for(int i=4; i<taskList.size(); i++) {
            logger.debug(i + ": " + taskList.get(i).name());
            tasks.put(taskList.get(i).name(),
                    updateService.addTaskAsChild(taskTwoId, taskList.get(i)).child());
        }

        tasks.put("Two", queryService.getTask(tasks.get("Two").id()));
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

    @Test
    void testTaskLinkOrder() {
        Task task2 = tasks.get("Two");
        Task task21 = tasks.get("TwoOne");
        Task task22 = tasks.get("TwoTwo");
        Task task23 = tasks.get("TwoThree");

        assertEquals(1, task2.parentLinks().size());
        assertEquals(3, task2.childLinks().size());
        assertEquals(Arrays.asList(task21.id(), task22.id(), task23.id()),
                task2.childLinks().stream().map(TaskLink::childId).toList());

        assertEquals(1, task21.parentLinks().size());
        assertEquals(1, task22.parentLinks().size());
        assertEquals(1, task23.parentLinks().size());

        List<TaskLinkEntity> links = mapper.toEntity(task2).childLinks();
        for(int i=0; i<links.size(); i++) {
            assertEquals(i, links.get(i).position());
        }
    }

    void validateLink(List<Task> expectedChildren, Task parent, Task child) {
        Set<Long> expectedParent = Set.of(parent.id());
        List<Long> expectedChildIds = expectedChildren.stream().map(Task::id).toList();
        assertEquals(expectedChildIds, parent.childLinks().stream().map(TaskLink::childId).toList());
        assertEquals(expectedParent, parent.childLinks().stream().map(TaskLink::parentId).collect(Collectors.toSet()));
        assertTrue(parent.childLinks().stream().anyMatch(link -> link.parentId().equals(parent.id())));
    }

    void validateLinkResponse(LinkResponse response, Task origParent, Task origChild) {
        Task parent = response.parent();
        Task child = response.child();
        TaskLink link = response.link();

        assertEquals(parent.id(), origParent.id());
        assertEquals(parent.id(), link.parentId());
        assertEquals(child.id(), link.childId());
        if (origChild.id() != null) {
            assertEquals(child.id(), origChild.id());
        }
    }

    @Test
    void testSaveTaskAsChild() {
        Task fresh = Task.builder().name("Fresh").build();
        Task parent = tasks.get("Two");

        LinkResponse response = updateService.addTaskAsChild(parent.id(), fresh);
        validateLinkResponse(response, parent, fresh);
        fresh = response.child();

        parent = queryService.getTask(parent.id());
        List<Long> children = parent.childLinks().stream().map(TaskLink::childId).toList();

        assertEquals(4, children.size());
        assertEquals(tasks.get("TwoOne").id(), children.get(0));
        assertEquals(tasks.get("TwoTwo").id(), children.get(1));
        assertEquals(tasks.get("TwoThree").id(), children.get(2));
        assertEquals(fresh.id(), children.get(3));

        List<Task> expectedChildren = List.of(tasks.get("TwoOne"), tasks.get("TwoTwo"), tasks.get("TwoThree"), fresh);
        validateLink(expectedChildren, parent, fresh);
    }

    @Test
    void testAddTaskAsChildAt() {
        Task fresh = Task.builder().name("Fresh").build();
        Task parent = tasks.get("Two");

        LinkResponse response = updateService.addTaskAsChildAt(1, parent.id(), fresh);
        validateLinkResponse(response, parent, fresh);

        parent = queryService.getTask(parent.id());
        fresh = response.child();

        List<Task> expectedChildren = List.of(tasks.get("TwoOne"), fresh, tasks.get("TwoTwo"), tasks.get("TwoThree"));
        validateLink(expectedChildren, parent, fresh);
    }

    @Test
    void testSaveTaskAsRoot() {
        Task fresh = Task.builder().name("Fresh").build();

        LinkResponse response = updateService.addTaskAsRoot(fresh);
        fresh = response.child();

        assertEquals("Fresh", fresh.name());
        assertFalse(fresh.parentLinks().isEmpty());
        assertTrue(fresh.childLinks().isEmpty());
    }

    @Test
    void testUpdateTask() {
        Task task = tasks.get("One");
        task.name("Updated");

        TaskResponse response = updateService.updateTask(task);

        Task updatedTask = response.task();
        assertNotNull(updatedTask);

        assertEquals("Updated", updatedTask.name());
    }

    @Test
    void testDeleteTask() {
        Task task = tasks.get("Two");
        Response response = updateService.deleteTask(task.id());

        assertTrue(response.success());

        TaskEntity deletedTask = taskRepository.findById(task.id()).orElse(null);
        assertNull(deletedTask);
    }

    @Test
    void testDeleteLink() {
        Task task2 = tasks.get("Two");
        Task task21 = tasks.get("TwoOne");

        TaskLink link = task21.parentLinks().get(0);
        LinkResponse r = updateService.deleteLink(link.id());

        assertTrue(r.success());

        assertThrows(IllegalArgumentException.class, () -> entityQueryService.getLink(link.id()));

        TaskEntity entity2 = mapper.toEntity(task2);
        TaskEntity entity21 = mapper.toEntity(task21);
        
        // Verify task2 no longer has task21 as a child, and vice-versa
        assertFalse(entity2.children().anyMatch(entity -> entity.equals(entity21)));
        assertFalse(entity2.childLinks().stream()
                .map(TaskLinkEntity::child).anyMatch(entity -> entity.equals(entity21)));
        assertFalse(entity21.parents().anyMatch(entity -> entity.equals(entity2)));
        assertFalse(entity21.parentLinks().stream()
                .map(TaskLinkEntity::parent).anyMatch(entity -> entity.equals(entity2)));
    }

    @Test
    public void testMoveTaskLink() {
        Task task1 = tasks.get("One");
        Task task2 = tasks.get("Two");
        Task task21 = tasks.get("TwoOne");

        TaskLink taskLink3 = task21.parentLinks().get(0);
        updateService.addTaskAsChild(task1.id(), task21);
        updateService.deleteLink(taskLink3.id());

        TaskEntity entity1 = mapper.toEntity(task1);
        TaskEntity entity2 = mapper.toEntity(task2);
        TaskEntity entity21 = mapper.toEntity(task21);

        // Verify that task3 is now a child of task1, and vice-versa
        assertTrue(entity1.children().anyMatch(entity -> entity.equals(entity21)));
        assertTrue(entity1.childLinks().stream()
                .map(TaskLinkEntity::child).anyMatch(entity -> entity.equals(entity21)));

        assertTrue(entity21.parents().anyMatch(entity -> entity.equals(entity1)));
        assertTrue(entity21.parentLinks().stream()
                .map(TaskLinkEntity::parent).anyMatch(entity -> entity.equals(entity1)));

        // Verify that task3 is no longer a child of task2
        assertFalse(entity2.children().anyMatch(entity -> entity.equals(entity21)));
        assertFalse(entity2.childLinks().stream()
                .map(TaskLinkEntity::child).anyMatch(entity -> entity.equals(entity21)));

        assertFalse(entity21.parents().anyMatch(entity -> entity.equals(entity2)));
        assertFalse(entity21.parentLinks().stream()
                .map(TaskLinkEntity::parent).anyMatch(entity -> entity.equals(entity2)));
    }

    @Test
    public void testTaskLinkOrderAfterChange() {
        // Get existing tasks from map
        Task task4 = tasks.get("Four");
        Task task21 = tasks.get("TwoOne");
        Task task22 = tasks.get("TwoTwo");
        Task task3 = tasks.get("Three");

        LinkResponse r21 = updateService.addTaskAsChild(task4.id(), task21);
        LinkResponse r22 = updateService.addTaskAsChild(task4.id(), task22);
        LinkResponse r3 = updateService.addTaskAsChildAt(r22.link().position(), task4.id(), task3);

        task21 = r21.child();
        task22 = r22.child();
        task3 = r3.child();
        assertEquals(r21.parent(), r22.parent());
        assertEquals(r22.parent(), r3.parent());

        task4 = queryService.getTask(task4.id());
        assertEquals(task4, r3.parent());

        assertEquals(3, task4.childLinks().size());
        List<Task> expectedChildren = Arrays.asList(task21, task3, task22);
        validateLink(expectedChildren, task4, task21);
        validateLink(expectedChildren, task4, task3);
        validateLink(expectedChildren, task4, task22);

        List<TaskLink> links = task4.childLinks();
        for(int i=0; i<links.size(); i++) {
            assertEquals(i, links.get(i).position());
        }
    }


}
