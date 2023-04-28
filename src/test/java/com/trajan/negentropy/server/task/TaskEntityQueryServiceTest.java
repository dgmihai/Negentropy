package com.trajan.negentropy.server.task;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.entity.TaskEntity_;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.backend.repository.filter.QueryOperator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TaskEntityQueryServiceTest {

    @Autowired private TaskEntityQueryService entityQueryService;

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;

    @PersistenceContext private EntityManager entityManager;

    Map<String, TaskEntity> tasks = new HashMap<>();
    Map<String, TaskLinkEntity> links = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Populate test data
        tasks.put("One", new TaskEntity("One"));
        tasks.put("Two", new TaskEntity("Two"));
        tasks.put("Three", new TaskEntity("Three"));
        tasks.put("Four", new TaskEntity("Four"));
        tasks.put("TwoOne", new TaskEntity("TwoOne"));
        tasks.put("TwoTwo", new TaskEntity("TwoTwo"));
        tasks.put("TwoThree", new TaskEntity("TwoThree"));
        tasks.put("TwoTwoOne", new TaskEntity("TwoTwoOne"));
        tasks.put("TwoTwoTwo", new TaskEntity("TwoTwoTwo"));
        tasks.put("TwoTwoThree", new TaskEntity("TwoTwoThree"));

        tasks.values().forEach(task -> {
            TaskEntity savedTask = taskRepository.save(task);
            tasks.put(savedTask.name(), savedTask);
        });

        // TODO: A null parent comes up weird in descendants/ancestors
        
        // Create TaskLink for each Task
        TaskLinkEntity link1 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("One"))
                .parent(null)
                .build();
        links.put("One", link1);

        TaskLinkEntity link2 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("Two"))
                .parent(null)
                .build();
        links.put("Two", link2);

        TaskLinkEntity link3 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("Three"))
                .parent(null)
                .build();
        links.put("Three", link3);

        TaskLinkEntity link4 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("Four"))
                .parent(null)
                .build();
        links.put("Four", link4);

        TaskLinkEntity link21 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoOne"))
                .parent(tasks.get("Two"))
                .position(0)
                .build();
        links.put("TwoOne", link21);

        TaskLinkEntity link22 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoTwo"))
                .parent(tasks.get("Two"))
                .position(1)
                .build();
        links.put("TwoTwo", link22);

        TaskLinkEntity link23 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoThree"))
                .parent(tasks.get("Two"))
                .position(2)
                .build();
        links.put("TwoThree", link23);

        TaskLinkEntity link221 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoTwoOne"))
                .parent(tasks.get("TwoTwo"))
                .position(0)
                .build();
        links.put("TwoTwoOne", link221);

        TaskLinkEntity link222 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoTwoTwo"))
                .parent(tasks.get("TwoTwo"))
                .position(1)
                .build();
        links.put("TwoTwoTwo", link222);

        TaskLinkEntity link223 = new TaskLinkEntity().toBuilder()
                .child(tasks.get("TwoTwoThree"))
                .parent(tasks.get("TwoTwo"))
                .position(2)
                .build();
        links.put("TwoTwoThree", link223);

        links.values().forEach(link -> {
            TaskLinkEntity savedNode = linkRepository.save(link);
            links.put(savedNode.child().name(), savedNode);
        });
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

        linkRepository.deleteAll();
        taskRepository.deleteAll();

        session.doWork(connection -> {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
            }
        });
    }

    @Test
    public void testGetTaskWithExistingTask() {
        TaskEntity task = tasks.get("One");
        TaskEntity fetchedTask = entityQueryService.getTask(task.id());

        assertEquals(task.id(), fetchedTask.id());
        assertEquals(task.name(), fetchedTask.name());
    }

    @Test
    public void testGetTaskWithNonExistingTask() {
        assertThrows(IllegalArgumentException.class, () -> entityQueryService.getTask(-1L));
    }

    @Test
    public void testFindTasksFilterByName() {
        Filter filter = Filter.builder()
                .field(TaskEntity_.NAME)
                .operator(QueryOperator.EQUALS)
                .value("One")
                .build();

        List<TaskEntity> tasks = entityQueryService.findTasks(List.of(filter), new HashSet<>()).toList();

        assertEquals(1, tasks.size());
        assertEquals("One", tasks.get(0).name());
    }

    @Test
    public void testFindTasksWithNoMatchingFilters() {
        Filter filter = Filter.builder()
                .field(TaskEntity_.NAME)
                .operator(QueryOperator.EQUALS)
                .value("NonExistent")
                .build();

        List<TaskEntity> tasks = entityQueryService.findTasks(List.of(filter), new HashSet<>()).toList();

        assertEquals(0, tasks.size());
    }

    @Test
    public void testFindTasksWithMultipleFilters() {
        Filter filter1 = Filter.builder()
                .field(TaskEntity_.NAME)
                .operator(QueryOperator.LIKE)
                .value("TwoTwo")
                .build();

        Filter filter2 = Filter.builder()
                .field(TaskEntity_.NAME)
                .operator(QueryOperator.LIKE)
                .value("One")
                .build();

        List<TaskEntity> tasks = entityQueryService.findTasks(List.of(filter1, filter2), new HashSet<>()).toList();

        assertEquals(1, tasks.size());
        assertEquals("TwoTwoOne", tasks.get(0).name());
    }

    @Test
    public void testFindTasksWithEmptyFilters() {
        List<TaskEntity> tasks = entityQueryService.findTasks(new ArrayList<>(), new HashSet<>()).toList();

        assertEquals(10, tasks.size());
    }

    @Test
    public void testGetChildrenOfTaskWithChildren() {
        TaskEntity parent = tasks.get("Two");
        parent = entityQueryService.getTask(parent.id());

        List<TaskEntity> children = parent.childLinks().stream()
                .map(TaskLinkEntity::child)
                .toList();

        assertEquals(3, children.size());
        assertTrue(children.stream().anyMatch(task -> task.name().equals("TwoOne")));
        assertTrue(children.stream().anyMatch(task -> task.name().equals("TwoTwo")));
    }

    @Test
    public void testGetChildrenOfTaskWithNoChildren() {
        TaskEntity parent = tasks.get("One");
        parent = entityQueryService.getTask(parent.id());

        List<TaskEntity> children = parent.childLinks().stream()
                .map(TaskLinkEntity::child)
                .toList();

        assertEquals(0, children.size());
    }

    @Test
    public void testCountChildrenOfTaskWithChildren() {
        TaskEntity parent = tasks.get("Two");
        parent = entityQueryService.getTask(parent.id());

        int childCount = entityQueryService.countChildren(parent);

        assertEquals(3, childCount);
    }

    @Test
    public void testCountChildrenOfTaskWithNoChildren() {
        TaskEntity parent = tasks.get("One");
        parent = entityQueryService.getTask(parent.id());

        int childCount = entityQueryService.countChildren(parent);

        assertEquals(0, childCount);
    }

    @Test
    public void testHasChildrenForTaskWithChildren() {
        TaskEntity parent = tasks.get("Two");
        parent = entityQueryService.getTask(parent.id());

        boolean hasChildren = entityQueryService.hasChildren(parent);

        assertTrue(hasChildren);
    }

    @Test
    public void testHasChildrenForTaskWithNoChildren() {
        TaskEntity parent = tasks.get("One");
        parent = entityQueryService.getTask(parent.id());

        boolean hasChildren = entityQueryService.hasChildren(parent);

        assertFalse(hasChildren);
    }

    @Test
    public void testGetParentsOfTaskWithParents() {
        TaskEntity child = tasks.get("TwoTwo");
        child = entityQueryService.getTask(child.id());

        List<TaskEntity> parents = child.parentLinks().stream()
                .map(TaskLinkEntity::parent)
                .toList();

        assertEquals(1, parents.size());
        assertEquals("Two", parents.get(0).name());
    }

    @Test
    public void testGetParentsOfTaskWithNoParents() {
        TaskEntity child = tasks.get("One");
        child = entityQueryService.getTask(child.id());

        List<TaskEntity> parents = child.parentLinks().stream()
                .map(TaskLinkEntity::parent)
                .toList();

        assertEquals(1, parents.size());
    }

    @Test
    public void testHasParentsForTaskWithParents() {
        TaskEntity child = tasks.get("TwoTwo");
        child = entityQueryService.getTask(child.id());

        boolean hasParents = entityQueryService.hasParents(child);

        assertTrue(hasParents);
    }

    @Test
    public void testHasParentsForTaskWithNoParents() {
        TaskEntity child = tasks.get("One");
        child = entityQueryService.getTask(child.id());

        boolean hasParents = entityQueryService.hasParents(child);

        List<TaskLinkEntity> alllinks = linkRepository.findAll();
        assertFalse(hasParents);
    }

    @Test
    public void testGetAncestorsOfTaskWithAncestors() {
        TaskEntity descendant = tasks.get("TwoTwoOne");
        descendant = entityQueryService.getTask(descendant.id());

        List<TaskEntity> ancestors = entityQueryService.getAncestors(descendant).toList();

        assertEquals(3, ancestors.size());
        assertTrue(ancestors.stream().anyMatch(task -> task.name().equals("Two")));
        assertTrue(ancestors.stream().anyMatch(task -> task.name().equals("TwoTwo")));
    }

    @Test
    public void testGetAncestorsOfRootTask() {
        TaskEntity descendant = tasks.get("One");
        descendant = entityQueryService.getTask(descendant.id());

        List<TaskEntity> ancestors = entityQueryService.getAncestors(descendant).toList();

        assertEquals(1, ancestors.size());
    }

    @Test
    public void testGetDescendantsOfTaskWithDescendants() {
        TaskEntity ancestor = tasks.get("Two");
        ancestor = entityQueryService.getTask(ancestor.id());

        List<TaskEntity> descendants = entityQueryService.getDescendants(ancestor).toList();

        assertEquals(6, descendants.size());
        assertEquals("TwoOne", descendants.get(0).name());
        assertEquals("TwoTwo", descendants.get(1).name());
        assertEquals("TwoTwoOne", descendants.get(2).name());
        assertEquals("TwoTwoTwo", descendants.get(3).name());
        assertEquals("TwoTwoThree", descendants.get(4).name());
        assertEquals("TwoThree", descendants.get(5).name());
    }

    @Test
    public void testGetDescendantsOfTaskWithNoDescendants() {
        TaskEntity ancestor = tasks.get("One");
        ancestor = entityQueryService.getTask(ancestor.id());

        List<TaskEntity> descendants = entityQueryService.getDescendants(ancestor).toList();

        assertEquals(0, descendants.size());
    }

}
