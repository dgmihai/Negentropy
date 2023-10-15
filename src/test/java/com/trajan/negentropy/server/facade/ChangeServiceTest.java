package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.model.sync.Change.CopyChange.CopyType;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.netduration.NetDurationHelperManager;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class ChangeServiceTest extends TaskTestTemplate {

    @BeforeAll
    void setup() {
        init();
        TaskID twotwotwoId = tasks.get(TWOTWOTWO).id();
        assertEquals(Duration.ofMinutes(1), queryService.fetchNetDuration(twotwotwoId, null));
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

    private TaskNode insertTaskInto(TaskID taskId, TaskID parentId, InsertLocation location) {
        return (TaskNode) execute(new InsertIntoChange(
                new TaskNodeDTO()
                        .childId(taskId),
                parentId,
                location));
    }

    private TaskNode insertTask(TaskID taskId, LinkID referenceId, InsertLocation location) {
        return (TaskNode) execute(new InsertAtChange(
                new TaskNodeDTO()
                        .childId(taskId),
                referenceId,
                location));
    }

    @Test
    @Transactional
    void testPersistTaskAsChild() {
        Task parent = tasks.get(TWO);
        Task fresh = createTask("Fresh");

        TaskNode freshNode = persistTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(parent.id()));

        assertEquals(parent.id(), freshNode.parentId());
        fresh = freshNode.child();

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
    @Transactional
    void testInsertTaskAsChild() {
        Task parent = tasks.get(TWO);
        Task fresh = createTask("Fresh");

        TaskNode freshNode = insertTaskInto(
                fresh.id(),
                parent.id(),
                InsertLocation.LAST);

        assertEquals(parent.id(), freshNode.parentId());
        fresh = freshNode.child();

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
    @Transactional
    void testPersistTaskAsChildAt() {
        Task parent = tasks.get(TWO);
        Task fresh = createTask("Fresh");

        TaskNode freshNode = persistTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(parent.id())
                .position(1));

        assertEquals(parent.id(), freshNode.parentId());
        fresh = freshNode.child();

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
    @Transactional
    void testPersistTaskAsRoot() {
        Task fresh = createTask("Fresh");

        TaskNode freshNode = persistTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(null));

        assertNull(freshNode.parentId());

        assertTrue(queryService.fetchRootNodes()
                .anyMatch(node ->
                        node.child().id().equals(fresh.id())));
    }

    @Test
    @Transactional
    void testInsertTaskAsRoot() {
        Task fresh = createTask("Fresh");

        TaskNode freshNode = insertTaskInto(
                fresh.id(),
                null,
                InsertLocation.LAST);

        assertNull(freshNode.parentId());

        assertTrue(queryService.fetchRootNodes()
                .anyMatch(node ->
                        node.child().id().equals(fresh.id())));
    }

    @Test
    @Transactional
    void testUpdateTask() {
        Task task = tasks.get(ONE);
        task.name("Updated");

        Task updatedTask = mergeTask(task);
        assertNotNull(updatedTask);

        assertEquals("Updated", updatedTask.name());
        task = queryService.fetchTask(tasks.get(ONE).id());
        assertEquals("Updated", task.name());
    }

    @Test
    @Disabled
    @Transactional
    void testDeleteTask() {
        Task task = tasks.get(TWO);

//        Response response = changeService.deleteTask(new Request<>(task.changes()));
//        assertTrue(response.success());

        TaskEntity deletedTask = taskRepository.findById(task.id().val()).orElse(null);
        assertNull(deletedTask);
    }

    private void deleteNode(LinkID linkId) {
        execute(new DeleteChange<>(linkId));
    }

    @Test
    @Transactional
    void testDeleteLink() {
        Task task2 = tasks.get(TWO);
        Task task21 = tasks.get(TWOONE);
        TaskLink link = entityQueryService.getTask(task21.id())
                .parentLinks().get(0);

        deleteNode(ID.of(link));

        assertThrows(NoSuchElementException.class,
                () -> entityQueryService.getLink(ID.of(link)));

        List<Object> expectedChildTasks = List.of(
                tasks.get(TWOTWO),
                tasks.get(TWOTHREE));

        validateNodes(
                queryService.fetchChildNodes(task2.id()),
                expectedChildTasks);
    }

    private TaskNode moveNode(LinkID linkId, LinkID newParentId, InsertLocation location) {
        return (TaskNode) execute(new MoveChange(linkId, newParentId, location));
    }

    @Test
    @Transactional
    void testMoveTaskLink() {
        Task task1 = tasks.get(ONE);
        TaskLink taskLink1 = entityQueryService.getTask(task1.id())
                .parentLinks().get(0);
        Task task2 = tasks.get(TWO);
        Task task21 = tasks.get(TWOONE);
        TaskLink taskLink3 = entityQueryService.getTask(task21.id())
                .parentLinks().get(0)
                .cron("@daily")
                .scheduledFor(LocalDateTime.MIN);

        TaskNode newNode = moveNode(ID.of(taskLink3),
                ID.of(taskLink1),
                InsertLocation.FIRST);

        assertEquals(task1.id(), newNode.parentId());
        // Moving shouldn't reset the scheduled time
        assertEquals(LocalDateTime.MIN, newNode.scheduledFor());

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
    @Transactional
    void testTaskLinkOrderAfterChange() {
        Task task4 = tasks.get(FOUR);
        Task task21 = tasks.get(TWOONE);
        Task task22 = tasks.get(TWOTWO);
        Task task33 = tasks.get(THREETHREE);

        persistTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task21.id()));
        TaskNode persisted = persistTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task22.id()));
        persistTaskNode(new TaskNodeDTO()
                .parentId(task4.id())
                .childId( task33.id())
                .position(persisted.position()));

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
    void testCyclicalConnectionDirect() {
        Task task2 = tasks.get(TWO);
        Task task22 = tasks.get(TWOTWO);

        DataMapResponse response = changeService.execute(Request.of(new PersistChange<>(new TaskNodeDTO()
                .parentId(task22.id())
                .childId( task2.id()))));

        assertFalse(response.success());
    }

    @Test
    void testCyclicalConnectionNested() {
        Task task2 = tasks.get(TWO);
        Task task222 = tasks.get(TWOTWOTWO);

        DataMapResponse response = changeService.execute(Request.of(new PersistChange<>(new TaskNodeDTO()
                .parentId(task222.id())
                .childId( task2.id()))));

        assertFalse(response.success());
    }

    @Test
    void testCyclicalConnectionSelf() {
        Task task1 = tasks.get(ONE);

        DataMapResponse response = changeService.execute(Request.of(new PersistChange<>(new TaskNodeDTO()
                .parentId(task1.id())
                .childId( task1.id()))));

        assertFalse(response.success());
    }

    @Test
    @Transactional
    void testInsertTaskBefore() {
        Task fresh = new Task().name("Fresh");
        Task next = tasks.get(TWOTWO);
        Task parent = tasks.get(TWO);

        TaskNode node22 = queryService.fetchChildNodes(parent.id())
                .filter(node -> node.child().id().equals(next.id()))
                .findFirst().orElseThrow();

        fresh = persistTask(fresh);

        TaskNode freshNode = insertTask(
                fresh.id(),
                node22.id(),
                InsertLocation.BEFORE);

        assertEquals(parent.id(), freshNode.parentId());
        fresh = queryService.fetchTask(freshNode.child().id());

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
    @Transactional
    void testInsertTaskAfter() {
        Task fresh = new Task().name("Fresh");
        Task prev = tasks.get(TWOONE);
        Task parent = tasks.get(TWO);

        TaskNode node21 = queryService.fetchChildNodes(parent.id())
                .filter(node -> node.child().id().equals(prev.id()))
                .findFirst().orElseThrow();

        fresh = persistTask(fresh);

        TaskNode freshNode = insertTask(
                fresh.id(),
                node21.id(),
                InsertLocation.AFTER);

        assertEquals(parent.id(), freshNode.parentId());
        fresh = queryService.fetchTask(freshNode.child().id());

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
    void testPartialUpdate() {
        TaskID oneId = tasks.get(ONE).id();
        String desc = "Partial Update";

        Task one = mergeTask(new Task(oneId).description(desc));

        assertEquals(ONE, one.name());
        assertEquals(Duration.ofMinutes(1), one.duration());
    }

    // NET DURATIONS

    void assertNetDuration(int expectedMinutes, String taskName) {
        TaskID id = tasks.get(taskName).id();

        assertEquals(
                Duration.ofMinutes(expectedMinutes),
                queryService.fetchNetDuration(id, null));
    }

    @Test
    void testNetDurationOfTaskWithNoDescendants() {
        assertNetDuration(1, ONE);
    }

    @Test
    void testNetDurationOfTaskWithSingleLevelOfDescendants() {
        assertNetDuration(4, TWOTWO);
    }

    @Test
    void testNetDurationOfTaskWithMultipleLevelsOfDescendants() {
        assertNetDuration(7, TWO);
    }

    @Test
    void testNetDurationOfTaskWithDuplicateDescendants() {
        assertNetDuration(5, THREETWO);
    }

    @Test
    void testNetDurationOfTaskWithComplexHierarchy() {
        assertNetDuration(8, THREE_AND_FIVE);
    }

    @Test
    @Transactional
    void testNetDurationAdjustedOnRemoval() {
        TaskLink link = links.get(Triple.of(
                TWOTWO, TWOTWOONE, 0));

        deleteNode(ID.of(link));

        assertNetDuration(3, TWOTWO);
        assertNetDuration(6, TWO);
    }

    @Test
    @Transactional
    void testNetDurationAdjustedOnRemovalNested() {
        TaskLink link = links.get(Triple.of(
                THREETWO, THREETWOONE_AND_THREETWOTHREE, 0));

        deleteNode(ID.of(link));

        assertNetDuration(7, THREE_AND_FIVE);

        link = links.get(Triple.of(
                THREETWO, THREETWOONE_AND_THREETWOTHREE, 2));

        deleteNode(ID.of(link));

        assertNetDuration(6, THREE_AND_FIVE);
    }

    @Test
    @Transactional
    void testNetDurationOnTaskUpdate() {
        Task task222 = tasks.get(TWOTWOTWO);

        mergeTask(task222.duration(Duration.ofMinutes(3)));

        assertNetDuration(9, TWO);
        assertNetDuration(6, TWOTWO);

        mergeTask(task222.duration(Duration.ofMinutes(1)));

        assertNetDuration(7, TWO);
        assertNetDuration(4, TWOTWO);
    }

    @Test
    @Transactional
    void testNetDurationWhenTaskWithNoDurationAdded() {
        Task task22 = tasks.get(TWOTWO);
        Task fresh = new Task()
                .name("Fresh");

        fresh = persistTask(fresh);
        persistTaskNode(new TaskNodeDTO()
                .parentId(task22.id())
                .childId(fresh.id()));

        assertNetDuration(7, TWO);
        assertNetDuration(4, TWOTWO);

        Task fresh2 = persistTask(new Task()
                .name("Fresh 2"));
        persistTaskNode(new TaskNodeDTO()
                .parentId(task22.id())
                .childId(fresh2.id())
                .position(1));

        assertNetDuration(7, TWO);
        assertNetDuration(4, TWOTWO);

        Task fresh3 = persistTask(new Task()
                .name("Fresh 3")
                .duration(Duration.ofMinutes(2)));
        persistTaskNode(new TaskNodeDTO()
                .parentId(task22.id())
                .childId(fresh3.id()));

        assertNetDuration(9, TWO);
        assertNetDuration(6, TWOTWO);
    }

    @Test
    @Transactional
    void testNetDurationWhenParentHasNoDuration() {
        Task task22 = tasks.get(TWOTWO);
        Task parent = persistTask(new Task()
                .name("Parent"));

        persistTaskNode(new TaskNodeDTO()
                .parentId(task22.id())
                .childId(parent.id()));

        Task child1 = persistTask(new Task()
                .name("Child 1")
                .duration(Duration.ofMinutes(2)));
        Task child2 = persistTask(new Task()
                .name("Child 2")
                .duration(Duration.ofMinutes(2)));

        persistTaskNode(new TaskNodeDTO()
                .parentId(parent.id())
                .childId(child1.id()));
        persistTaskNode(new TaskNodeDTO()
                .parentId(parent.id())
                .childId(child2.id()));

        assertNetDuration(11, TWO);
        assertNetDuration(8, TWOTWO);
        assertEquals(
                Duration.ofMinutes(4),
                queryService.fetchNetDuration(parent.id(), null));
    }

    @Autowired private NetDurationHelperManager netDurationHelperManager;

    @Test
    void testRecalculateNetDurations() {
        assertNetDuration(1, ONE);
        assertNetDuration(4, TWOTWO);
        assertNetDuration(7, TWO);
        assertNetDuration(5, THREETWO);
        assertNetDuration(8, THREE_AND_FIVE);
        assertNetDuration(1, FOUR);
        assertNetDuration(1, SIX_AND_THREETWOFOUR);

        netDurationHelperManager.recalculateTimeEstimates();

        assertNetDuration(1, ONE);
        assertNetDuration(4, TWOTWO);
        assertNetDuration(7, TWO);
        assertNetDuration(5, THREETWO);
        assertNetDuration(8, THREE_AND_FIVE);
        assertNetDuration(1, FOUR);
        assertNetDuration(1, SIX_AND_THREETWOFOUR);
    }

    private void assertDeepCopy(TaskNode root, List<String> expectedNames, TaskNodeTreeFilter filter) {
        String suffix = " (copy)";

        List<String> expected = expectedNames.stream()
                .map(taskName -> taskName + suffix)
                .toList();

        Change deepCopy = new CopyChange(
                CopyType.DEEP,
                root.linkId(),
                null,
                InsertLocation.LAST,
                filter,
                suffix);
        ChangeID id = deepCopy.id();

        DataMapResponse response = changeService.execute(Request.of(deepCopy));
        TaskNode newRootNode = (TaskNode) response.changeRelevantDataMap().getFirst(id);

        List<String> results = queryService.fetchDescendantNodes(newRootNode.childId(), filter)
                .map(taskNode -> taskNode.child().name())
                .toList();

        assertEquals(expected, results);
        assertEquals(expected.size(), results.size());
    }

    @Test
    @Transactional
    @Disabled
    void testDeepCopyNode() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));

        List<String> taskNames = List.of(
                TWO,
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO,
                TWOTHREE);

        assertDeepCopy(root, taskNames, null);
    }

    @Test
    @Transactional
    @Disabled
    void testDeepCopyNodeWithFilter() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter(
                TaskNodeTreeFilter.ONLY_REQUIRED,
                TaskNodeTreeFilter.ALWAYS_INCLUDE_PARENTS);

        List<String> taskNames = List.of(
                TWO,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO);

        assertDeepCopy(root, taskNames, filter);
    }

    @Test
    @Transactional
    void testUpdateNodes() {
        Task root = tasks.get(TWO);

        List<LinkID> linkIds = entityQueryService.findDescendantLinks(root.id(), null)
                .map(ID::of)
                .toList();

        TaskNodeTemplateData nodeDTO = new TaskNodeDTO()
                .completed(true)
                .importance(5);

        SyncResponse response = changeService.execute(Request.of(
                new MultiMergeChange<>(nodeDTO, linkIds)));
        assertTrue(response.success());

        Consumer<TaskNode> assertNodes = node -> {
            assertTrue(node.completed());
            assertEquals(5, node.importance());
        };

        queryService.fetchDescendantNodes(root.id(), null)
                .forEach(assertNodes);

        queryService.fetchDescendantNodes(root.id(), null)
                .forEach(assertNodes);
    }

    @Test
    @Transactional
    void testClearNodeCron() {
        TaskNode two = nodes.get(Triple.of(NULL, TWO, 1));
        assertNotNull(two.cron());

        Change change = new MergeChange<>(two
                .cron(CronExpression.parse(K.NULL_CRON)));

        DataMapResponse response = changeService.execute(Request.of(change));

        assertTrue(response.success());

        TaskNode updated = (TaskNode) response.changeRelevantDataMap().get(change.id()).get(0);
        assertNull(updated.cron());
    }

}