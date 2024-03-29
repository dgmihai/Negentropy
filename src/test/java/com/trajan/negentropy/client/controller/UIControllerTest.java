package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.ID.TaskOrLinkID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MoveChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIControllerTest extends ClientTestTemplate {

    @BeforeAll
    void setup() {
        init();
        controller = new TestUIController(testServices);
    }

    @Override
    protected void validateNodes(Stream<TaskNode> nodes, List<Object> expected) {
        List<TaskNode> nodeList = nodes
                .toList();
        System.out.println("EXPECTED: " + expected.stream()
                .map(obj -> {
                    if (obj instanceof Task t) {
                        return t.name();
                    } else if (obj instanceof String s) {
                        return s;
                    } else if (obj instanceof TaskNode n) {
                        return n.task().name();
                    } else {
                        throw new RuntimeException();
                    }
                })
                .toList());
        System.out.println("ACTUAL: " + nodeList.stream()
                .map(node -> node.task().name())
                .toList());

        TaskID parentId = nodeList.get(0).parentId();

        for (int i=0; i<nodeList.size(); i++) {
            TaskNode node = nodeList.get(i);
            Task task;
            Object obj = expected.get(i);
            if (obj instanceof Task t) {
                task = t;
            } else if (obj instanceof String s) {
                task = tasks.get(s);
            } else if (obj instanceof TaskNode n) {
                task = n.task();
            } else {
                throw new RuntimeException();
            }

            assertEquals(task.id(), node.child().id());
            assertEquals(parentId, node.parentId());
        }
    }
    
    TaskNode testTaskInserted(TaskOrLinkID reference, InsertLocation location, int position, String parent) {
        SyncID syncId = queryService.currentSyncId();
        System.out.println("Initial sync id: " + syncId);
        assertNotNull(syncId);

        TaskNode resultNode;

        if (reference == null) {
            resultNode = taskNodeProvider.createNode_TEST(
                    null,
                    location);
        } else if (reference instanceof TaskID taskReference) {
            resultNode = taskNodeProvider.createNode_TEST(
                    taskReference,
                    location);
        } else if (reference instanceof LinkID linkReference) {
            resultNode = taskNodeProvider.createNode_TEST(
                    linkReference,
                    location);
        } else  {
            throw new RuntimeException("Invalid reference type");
        }

        assertTaskInserted(position, parent, resultNode);
        assertNotEquals(syncId, queryService.currentSyncId());

        return resultNode;
    }

    @Test
    void testInsertNode_AddFirstToRoot() {
        TaskNode resultNode = testTaskInserted(
                null, 
                InsertLocation.FIRST, 
                0, 
                null);
        
        List<Object> expectedTasks = List.of(
                Objects.requireNonNull(resultNode),
                ONE,
                TWO,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddLastToRoot() {
        TaskNode resultNode = testTaskInserted(
                null, 
                InsertLocation.LAST,
                6, 
                null);

        List<Object> expectedTasks = List.of(
                ONE,
                TWO,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR,
                Objects.requireNonNull(resultNode));

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddChildToRoot() {
        TaskNode resultNode = testTaskInserted(
                null, 
                InsertLocation.CHILD,
                6, 
                null);
        
        List<Object> expectedTasks = List.of(
                ONE,
                TWO,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR,
                Objects.requireNonNull(resultNode));

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddBeforeRootTask() {
        TaskNode resultNode = testTaskInserted(
                nodes.get(Triple.of(NULL, ONE, 0)).linkId(),
                InsertLocation.BEFORE,
                0, 
                null);
        
        List<Object> expectedTasks = List.of(
                Objects.requireNonNull(resultNode),
                ONE,
                TWO,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddAfterRootTask() {
        TaskNode resultNode = testTaskInserted(
                nodes.get(Triple.of(NULL, TWO, 1)).linkId(),
                InsertLocation.AFTER,
                2, 
                null);
        
        List<Object> expectedTasks = List.of(
                ONE,
                TWO,
                Objects.requireNonNull(resultNode),
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddFirstToParent() {
        TaskNode parentNode = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode resultNode = testTaskInserted(
                parentNode.childId(),
                InsertLocation.FIRST,
                0,
                TWO);

        List<Object> expectedTasks = List.of(
                Objects.requireNonNull(resultNode),
                TWOONE,
                TWOTWO,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(parentNode.child().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddLastToParent() {
        TaskNode parentNode = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode resultNode = testTaskInserted(
                parentNode.childId(),
                InsertLocation.LAST,
                3,
                TWO);

        List<Object> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                TWOTHREE,
                Objects.requireNonNull(resultNode));

        validateNodes(
                queryService.fetchChildNodes(parentNode.child().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddChildToParent() {
        TaskNode parentNode = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode resultNode = testTaskInserted(
                parentNode.childId(),
                InsertLocation.CHILD,
                3,
                TWO);

        List<Object> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                TWOTHREE,
                Objects.requireNonNull(resultNode));
        
        validateNodes(
                queryService.fetchChildNodes(parentNode.child().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddAfterTask() {
        TaskNode referenceNode = nodes.get(Triple.of(TWO, TWOTWO, 1));
        TaskNode resultNode = testTaskInserted(
                referenceNode.linkId(),
                InsertLocation.AFTER,
                2,
                TWO);

        List<Object> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                Objects.requireNonNull(resultNode),
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(referenceNode.parentId()),
                expectedTasks);
    }

    @Test
    void testInsertNode_AddBeforeTask() {
        TaskNode referenceNode = nodes.get(Triple.of(TWO, TWOTWO, 1));
        TaskNode resultNode = testTaskInserted(
                referenceNode.linkId(),
                InsertLocation.BEFORE,
                1,
                TWO);

        List<Object> expectedTasks = List.of(
                TWOONE,
                Objects.requireNonNull(resultNode),
                TWOTWO,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(referenceNode.parentId()),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveFirstOfReferenceTask() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode reference = nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2));

        Change change = new MoveChange(
                input.linkId(),
                reference.linkId(),
                InsertLocation.FIRST);

        DataMapResponse response = controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);

        expectedTasks = List.of(
                Objects.requireNonNull(resultNode),
                THREEONE,
                THREETWO,
                THREETHREE);

        validateNodes(
                queryService.fetchChildNodes(reference.task().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveFirstOfRoot() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));

        Change change = new MoveChange(
                input.linkId(),
                null,
                InsertLocation.FIRST);

        DataMapResponse response = controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                Objects.requireNonNull(resultNode),
                ONE,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveLastOfRoot() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));

        Change change = new MoveChange(
                input.linkId(),
                null,
                InsertLocation.LAST);

        DataMapResponse response = (DataMapResponse) controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR,
                Objects.requireNonNull(resultNode));

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveLastOfReferenceTask() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode reference = nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2));

        Change change = new MoveChange(
                input.linkId(),
                reference.linkId(),
                InsertLocation.LAST);

        DataMapResponse response = (DataMapResponse) controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);

        expectedTasks = List.of(
                THREEONE,
                THREETWO,
                THREETHREE,
                Objects.requireNonNull(resultNode));

        validateNodes(
                queryService.fetchChildNodes(reference.task().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveChildOfRoot() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));

        Change change = new PersistChange<>(input.toDTO()
                        .childId(input.child().id())
                        .parentId(null)
                        .position(4));

        DataMapResponse response = controller.requestChanges(List.of(
                change, new DeleteChange<>(input.linkId())));
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                THREE_AND_FIVE,
                FOUR,
                Objects.requireNonNull(resultNode),
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveChildOfReferenceTask() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode reference = nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2));

        Change change = new PersistChange<>(input.toDTO()
                .childId(input.child().id())
                .parentId(reference.childId())
                .position(2));

        DataMapResponse response = controller.requestChanges(List.of(
                change, new DeleteChange<>(input.linkId())));
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);

        expectedTasks = List.of(
                THREEONE,
                THREETWO,
                Objects.requireNonNull(resultNode),
                THREETHREE);

        validateNodes(
                queryService.fetchChildNodes(reference.task().id()),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveBeforeOfRoot() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));

        Change change = new MoveChange(
                input.linkId(),
                null,
                InsertLocation.BEFORE);

        Assertions.assertFalse(controller.requestChange(change).success());
    }

    @Test
    void testInsertNode_MoveBeforeOfReferenceTask() {
        TaskNode input = nodes.get(Triple.of(NULL, SIX_AND_THREETWOFOUR, 5));
        TaskNode reference = nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2));

        Change change = new MoveChange(
                input.linkId(),
                reference.linkId(),
                InsertLocation.BEFORE);

        DataMapResponse response = controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                TWO,
                Objects.requireNonNull(resultNode),
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }

    @Test
    void testInsertNode_MoveAfterOfRoot() {
        TaskNode input = nodes.get(Triple.of(NULL, TWO, 1));

        Change change = new MoveChange(
                input.linkId(),
                null,
                InsertLocation.AFTER);

        Assertions.assertFalse(controller.requestChange(change).success());
    }

    @Test
    void testInsertNode_MoveAfterOfReferenceTask() {
        TaskNode input = nodes.get(Triple.of(NULL, SIX_AND_THREETWOFOUR, 5));
        TaskNode reference = nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2));

        Change change = new MoveChange(
                input.linkId(),
                reference.linkId(),
                InsertLocation.AFTER);

        DataMapResponse response = (DataMapResponse) controller.requestChange(change);
        TaskNode resultNode = (TaskNode) response.changeRelevantDataMap().getFirst(change.id());

        List<Object> expectedTasks = List.of(
                ONE,
                TWO,
                THREE_AND_FIVE,
                Objects.requireNonNull(resultNode),
                FOUR,
                THREE_AND_FIVE);

        validateNodes(
                queryService.fetchChildNodes(null),
                expectedTasks);
    }
//
//    private void assertState(AtomicInteger state, int index, Object expected, Object actual) {
//        int stateValue = state.getAcquire();
//        if (stateValue == -1) {
//            if (!actual.equals(expected)) {
//                stateValue = index;
//            }
//        }
//        state.setRelease(stateValue);
//    }
//
//    @Test
//    void testKickStepUp_Root() {
//        AtomicInteger state = new AtomicInteger(-1);
//        controller.createRoutine(
//                tasks.get(TWOTWO),
//                new NonSpecificTaskNodeTreeFilter(),
//                response -> {
//                    assertState(state, 0, response.success(), true);
//
//                    Routine routine = response.routine();
//
//                    assertState(state, 2, 1, routine.children().size());
//
//                    assertState(state, 3,
//                            routine.getDescendants().stream()
//                                    .map(RoutineStep::name)
//                                    .toList(),
//                            List.of(
//                                    TWOTWO,
//                                    TWOTWOONE,
//                                    TWOTWOTWO,
//                                    TWOTWOTHREE_AND_THREETWOTWO));
//
//                    RoutineStep twoTwoTwo = routine.getDescendants().get(2);
//                    assertState(state, 4, TWOTWOTWO, twoTwoTwo.name());
//
//                    controller.kickUpStep(
//                            twoTwoTwo.id(),
//                            r -> {
//                                    assertState(state, 5, r.success(), true);
//                                    Routine updatedRoutine = r.routine();
//
//                                    assertState(state, 6, 2, updatedRoutine.children().size());
//                                    assertState(state, 7,
//                                            routine.getDescendants().stream()
//                                                    .map(RoutineStep::name)
//                                                    .toList(),
//                                            List.of(
//                                                    TWOTWO,
//                                                    TWOTWOTWO,
//                                                    TWOTWOONE,
//                                                    TWOTWOTWO,
//                                                    TWOTWOTHREE_AND_THREETWOTWO));
//
//                                    assertState(state, 8, TimeableStatus.EXCLUDED, updatedRoutine.getDescendants().get(3).status());
//                                    state.set(0);
//                                    System.out.println("HERE");
//                            },
//                            r -> {
//                                System.out.println(r.message());
//                                assertState(state, 9, false, true);
//                            });
//                    System.out.println("HERE2");
//                },
//                response -> {
//                    System.out.println(response.message());
//                    assertState(state, 10, false, true);
//                });
//
//        while(state.get() == -1) {
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Test
//    void testKickStepUp_Nested() {
//        controller.createRoutine(
//                tasks.get(TWO),
//                new NonSpecificTaskNodeTreeFilter(),
//                response -> {
//                    assertTrue(response.success());
//                    Routine routine = response.routine();
//                    assertEquals(1, routine.children().size());
//
//                    assertEquals(
//                            routine.getDescendants().stream()
//                                    .map(RoutineStep::name)
//                                    .toList(),
//                            List.of(
//                                        TWO,
//                                        TWOONE,
//                                        TWOTWO,
//                                        TWOTWOONE,
//                                        TWOTWOTWO,
//                                        TWOTWOTHREE_AND_THREETWOTWO,
//                                        TWOTHREE));
//
//                    RoutineStep twoTwoTwo = routine.getDescendants().get(4);
//                    assertEquals(TWOTWOTWO, twoTwoTwo.name());
//
//                    controller.kickUpStep(
//                            twoTwoTwo.id(),
//                            r -> {
//                                assertTrue(r.success());
//                                Routine updatedRoutine = r.routine();
//                                assertEquals(2, updatedRoutine.children().size());
//
//                                assertEquals(
//                                        routine.getDescendants().stream()
//                                                .map(RoutineStep::name)
//                                                .toList(),
//                                        List.of(
//                                                TWO,
//                                                TWOONE,
//                                                TWOTWO,
//                                                TWOTWOONE,
//                                                TWOTWOTWO,
//                                                TWOTWOTHREE_AND_THREETWOTWO,
//                                                TWOTHREE,
//                                                TWOTWOTWO));
//
//                                assertEquals(TimeableStatus.EXCLUDED, updatedRoutine.getDescendants().get(5).status());
//                            },
//                            r -> fail(r.message()));
//                },
//                response -> fail(response.message()));
//    }
}