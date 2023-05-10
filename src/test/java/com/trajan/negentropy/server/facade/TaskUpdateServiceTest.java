package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class TaskUpdateServiceTest extends TaskTestTemplate {

    @BeforeAll
    public void setup() {
        init();
    }

    private void validateNodes(Stream<TaskNode> nodes, List<Object> expected) {
        List<TaskNode> nodeList = nodes
                .peek(node -> System.out.println("Validate nodes peek: child=" + taskIds.get(node.childId())))
                .toList();
        System.out.println("EXPCTD: " + expected);
        TaskID parentId = nodeList.get(0).parentId();
        for (int i=0; i<nodeList.size(); i++) {
            TaskNode node = nodeList.get(i);
            Task task;
            Object obj = expected.get(i);
            if (obj instanceof Task t) {
                task = t;
            } else if (obj instanceof String s) {
                task = tasks.get(s);
            } else {
                throw new RuntimeException();
            }

            assertEquals(node.position(), i);
            assertEquals(task.id(), node.childId());
            assertEquals(parentId, node.parentId());
        }
    }

    @Test
    void testFullTree() {
        Collection<String> expectedTasks = List.of(
                ONE,
                TWO,
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO,
                TWOTHREE,
                THREE_AND_FIVE,
                THREEONE,
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                TWOTWOTHREE_AND_THREETWOTWO,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR,
                THREETHREE,
                FOUR,
                THREE_AND_FIVE,
                THREEONE,
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                TWOTWOTHREE_AND_THREETWOTWO,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR,
                THREETHREE,
                SIX_AND_THREETWOFOUR
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(NULL, ONE, 0),
                Triple.of(NULL, TWO, 1),
                Triple.of(TWO, TWOONE, 0),
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2),
                Triple.of(TWO, TWOTHREE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(THREE_AND_FIVE, THREEONE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETHREE, 2),
                Triple.of(NULL, FOUR, 3),
                Triple.of(NULL, THREE_AND_FIVE, 4),
                Triple.of(THREE_AND_FIVE, THREEONE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETHREE, 2),
                Triple.of(NULL, SIX_AND_THREETWOFOUR, 5)
        );

        testFindDescendantLinks(null, null, expectedLinks);
        testFindDescendantTasks(null, null, expectedTasks);
    }

    @Test
    void testSaveTaskAsChild() {
        Task fresh = new Task(null).name("Fresh");
        Task parent = tasks.get(TWO);

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(parent.id()));

        assertTrue(response.success());

        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.fetchTask(response.node().childId());

        List<Object> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                TWOTHREE,
                fresh);

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);
    }

    @Test
    void testAddTaskAsChildAt() {
        Task fresh = new Task(null).name("Fresh");
        Task parent = tasks.get(TWO);

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .position(1)
                .parentId(parent.id())
                .childId(fresh.id()));

        assertTrue(response.success());

        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.fetchTask(response.node().childId());

        List<Object> expectedTasks = List.of(
                TWOONE,
                fresh,
                TWOTWO,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);
    }

    @Test
    void testSaveTaskAsRoot() {
        Task f = new Task(null).name("Fresh");

        Task fresh = updateService.createTask(f).task();

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .childId(fresh.id()));

        assertNull(response.node().parentId());

        assertTrue(queryService.fetchRootNodes()
                .anyMatch(node ->
                        node.childId().equals(fresh.id())));
    }

    @Test
    void testUpdateTask() {
        Task task = tasks.get(ONE);
        task.name("Updated");

        TaskResponse response = updateService.updateTask(task);
        assertTrue(response.success());

        Task updatedTask = response.task();
        assertNotNull(updatedTask);

        assertEquals("Updated", updatedTask.name());
        task = queryService.fetchTask(tasks.get(ONE).id());
        assertEquals("Updated", task.name());
    }

    @Test
    @Disabled
    void testDeleteTask() {
        Task task = tasks.get(TWO);

        Response response = updateService.deleteTask(task.id());
        assertTrue(response.success());

        TaskEntity deletedTask = taskRepository.findById(task.id().val()).orElse(null);
        assertNull(deletedTask);
    }

    @Test
    @Transactional
    void testDeleteLink() {
        Task task2 = tasks.get(TWO);
        Task task21 = tasks.get(TWOONE);
        TaskLink link = entityQueryService.getTask(task21.id())
                .parentLinks().get(0);

        Response response = updateService.deleteNode(ID.of(link));
        assertTrue(response.success());

        assertThrows(NoSuchElementException.class,
                () -> entityQueryService.getLink(ID.of(link)));

        List<Object> expectedChildTasks = List.of(
                tasks.get(TWOTWO),
                tasks.get(TWOTHREE));

        validateNodes(
                queryService.fetchChildNodes(task2.id()),
                expectedChildTasks);
    }

    @Test
    @Transactional
    public void testMoveTaskLink() {
        Task task1 = tasks.get(ONE);
        Task task2 = tasks.get(TWO);
        Task task21 = tasks.get(TWOONE);
        TaskLink taskLink3 = entityQueryService.getTask(task21.id())
                .parentLinks().get(0);

        NodeResponse nodeResponse = updateService.insertTaskNode(new TaskNodeDTO()
                .position(0)
                .parentId(task1.id())
                .childId( task21.id()));

        Response response = updateService.deleteNode(ID.of(taskLink3));

        assertTrue(nodeResponse.success());
        assertTrue(response.success());

        assertEquals(task1.id(), nodeResponse.node().parentId());

        List<Object> expectedTask1ChildTasks = List.of(
                tasks.get(TWOONE));

        validateNodes(
                queryService.fetchChildNodes(task1.id()),
                expectedTask1ChildTasks);

        List<Object> expectedTask2ChildTasks = List.of(
                tasks.get(TWOTWO),
                tasks.get(TWOTHREE));
        validateNodes(
                queryService.fetchChildNodes(task2.id()),
                expectedTask2ChildTasks);
    }

    @Test
    public void testTaskLinkOrderAfterChange() {
        Task task4 = tasks.get(FOUR);
        Task task21 = tasks.get(TWOONE);
        Task task22 = tasks.get(TWOTWO);
        Task task33 = tasks.get(THREETHREE);

        NodeResponse r21 = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task21.id()));
        NodeResponse r22 = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task22.id()));
        NodeResponse r33 = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task33.id())
                .position(r22.node().position()));

        assertTrue(r21.success());
        assertTrue(r22.success());
        assertTrue(r33.success());

        List<Object> expectedChildren = List.of(
                TWOONE,
                THREETHREE,
                TWOTWO);

        validateNodes(
                queryService.fetchChildNodes(task4.id()),
                expectedChildren);

        long descendantCount = entityQueryService.findDescendantTasks(task4.id(), null).count();
        assertEquals(6, descendantCount);
    }

    @Test
    public void testCyclicalConnectionDirect() {
        Task task2 = tasks.get(TWO);
        Task task22 = tasks.get(TWOTWO);

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task22.id())
                .childId( task2.id()));

        assertFalse(response.success());
    }

    @Test
    public void testCyclicalConnectionNested() {
        Task task2 = tasks.get(TWO);
        Task task222 = tasks.get(TWOTWOTWO);

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task222.id())
                .childId( task2.id()));

        assertFalse(response.success());
    }

    @Test
    public void testCyclicalConnectionSelf() {
        Task task1 = tasks.get(ONE);

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(task1.id())
                .childId( task1.id()));

        assertFalse(response.success());
    }

    @Test
    public void testInsertTaskBefore() {
        Task fresh = new Task().name("Fresh");
        Task next = tasks.get(TWOTWO);
        Task parent = tasks.get(TWO);

        TaskNode node22 = queryService.fetchChildNodes(parent.id())
                .filter(node -> node.childId().equals(next.id()))
                .findFirst().orElseThrow();

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(node22.parentId())
                .childId( fresh.id())
                .position(node22.position()));

        assertTrue(response.success());

        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.fetchTask(response.node().childId());

        List<Object> expectedTasks = List.of(
                tasks.get(TWOONE),
                fresh,
                next,
                tasks.get(TWOTHREE));

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);
    }

    @Test
    public void testInsertTaskAfter() {
        Task fresh = new Task(null).name("Fresh");
        Task prev = tasks.get(TWOONE);
        Task parent = tasks.get(TWO);

        TaskNode node21 = queryService.fetchChildNodes(parent.id())
                .filter(node -> node.childId().equals(prev.id()))
                .findFirst().orElseThrow();

        fresh = updateService.createTask(fresh).task();

        NodeResponse response = updateService.insertTaskNode(new TaskNodeDTO()
                .parentId(node21.parentId())
                .childId( fresh.id())
                .position(node21.position() + 1));

        assertTrue(response.success());

        assertEquals(parent.id(), response.node().parentId());
        fresh = queryService.fetchTask(response.node().childId());

        List<Object> expectedTasks = List.of(
                prev,
                fresh,
                TWOTWO,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);
    }

    @Test
    public void testPartialUpdate() {
        TaskID oneId = tasks.get(ONE).id();
        String desc = "Partial Update";

        Task one = new Task(oneId).description(desc);
        one = updateService.updateTask(one).task();

        assertEquals(ONE, one.name());
        assertEquals(Duration.ofMinutes(1), one.duration());
    }

    // TIME ESTIMATES

    private void assertDuration(int minutes, String taskName) {
        TaskID id = tasks.get(taskName).id();

        assertEquals(
                Duration.ofMinutes(minutes),
                queryService.fetchNetTimeDuration(id));
    }

    @Test
    public void testDurationOfTaskWithNoDescendants() {
        assertDuration(1, ONE);
    }

    @Test
    public void testDurationOfTaskWithSingleLevelOfDescendants() {
        assertDuration(4, TWOTWO);
    }

    @Test
    public void testDurationOfTaskWithMultipleLevelsOfDescendants() {
        assertDuration(7, TWO);
    }

    @Test
    public void testDurationOfTaskWithDuplicateDescendants() {
        assertDuration(5, THREETWO);
    }

    @Test
    public void testDurationsOfTaskWithComplexHierarchy() {
        assertDuration(8, THREE_AND_FIVE);
    }

    @Test
    public void testTimeEstimateAdjustedOnRemoval() {
        TaskLink link = links.get(Triple.of(
                TWOTWO, TWOTWOONE, 0));

        updateService.deleteNode(ID.of(link));

        assertDuration(3, TWOTWO);
        assertDuration(6, TWO);
    }

    @Test
    public void testTimeEstimateAdjustedOnRemovalNested() {
        TaskLink link = links.get(Triple.of(
                THREETWO, THREETWOONE_AND_THREETWOTHREE, 0));

        updateService.deleteNode(ID.of(link));

        assertDuration(7, THREE_AND_FIVE);

        link = links.get(Triple.of(
                THREETWO, THREETWOONE_AND_THREETWOTHREE, 2));

        updateService.deleteNode(ID.of(link));

        assertDuration(6, THREE_AND_FIVE);
    }
}