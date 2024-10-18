package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.facade.response.Request;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class PositionFrozenTest extends TaskTestTemplate {

    @BeforeAll
    void setup() {
        init();
        TaskNode twoOne = nodes.get(Triple.of(TWO, TWOONE, 0));
        changeService.execute(Request.of(new MergeChange<>(
                twoOne.positionFrozen(true))));
        TaskNode twoThree = nodes.get(Triple.of(TWO, TWOTHREE, 2));
        changeService.execute(Request.of(new MergeChange<>(
                twoThree.positionFrozen(true))));

        assertTrue(queryService.fetchNode(twoOne.id()).positionFrozen());
        assertTrue(queryService.fetchNode(twoThree.id()).positionFrozen());
    }

    @Test
    @Transactional
    void testPersistAtFrozenStartingPosition() {
        Task parent = tasks.get(TWO);
        Task fresh = createTask("Fresh");

        TaskNode freshNode = persistTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(parent.id())
                .position(0));

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
    void testPersistAtFrozenEndingPosition() {
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
                fresh,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);
    }

    @Test
    @Transactional
    void testPersistNotAtStartOrEnd() {
        Task parent = tasks.get(TWO);
        Task fresh = createRequiredTask("Fresh");

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

        // Assert that the position is frozen, since that's implied when adding a new required link
        assertTrue(freshNode.positionFrozen());
    }

    @Test
    @Transactional
    void testDisallowPositionFrozenInMiddle() {
        Task parent = tasks.get(TWO);
        TaskNode twoThree = nodes.get(Triple.of(TWO, TWOTHREE, 2));
        changeService.execute(Request.of(new MergeChange<>(
                twoThree.positionFrozen(false))));

        Task fresh = createTask("Fresh");

        TaskNode freshNode = persistTaskNode(new TaskNodeDTO()
                .childId(fresh.id())
                .parentId(parent.id())
                .positionFrozen(true)
                .position(2));

        assertEquals(parent.id(), freshNode.parentId());
        fresh = freshNode.child();

        List<Object> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                fresh,
                TWOTHREE);

        validateNodes(
                queryService.fetchChildNodes(parent.id()),
                expectedTasks);

        assertFalse(freshNode.positionFrozen());
    }
}
