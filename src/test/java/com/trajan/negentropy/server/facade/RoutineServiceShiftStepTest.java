package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.RoutineTestTemplateWithRequiredTasks;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceShiftStepTest extends RoutineTestTemplateWithRequiredTasks {
    @BeforeAll
    void setup() {
        init();

        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwo.projectStepCountLimit(Optional.empty())
                                .projectDurationLimit(Optional.empty()))));
    }

    @Test
    @Transactional
    void testKickUpStep() { // Not recurring
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        RoutineResponse response = routineService.createRoutine(root.id(), null);

        assertTrue(response.success());
        Routine routine = response.routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        response = routineService.kickStepUp(routine.getDescendants().stream()
                        .filter(step -> step.name().equals(TWOTWOTWO))
                        .findFirst()
                        .get()
                        .id(),
                LocalDateTime.now());

        assertTrue(response.success());
        routine = response.routine();

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTWOTWO,
                        TWOTHREE),
                routine);

        assertEquals(TimeableStatus.NOT_STARTED, routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWOTWO))
                .findFirst()
                .get()
                .status());

        assertEquals(2, routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWO))
                .findFirst()
                .get()
                .children().size());

        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        assertEquals(2, queryService.fetchChildCount(twoTwo.childId(), null));
    }

    @Test
    @Transactional
    void testKickUpStep_Recurring() {
        TaskNode twoTwoTwo = nodes.get(Triple.of(TWOTWO, TWOTWOTWO, 1));
        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwoTwo.recurring(true))));

        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        RoutineResponse response = routineService.createRoutine(root.id(), null);

        assertTrue(response.success());
        Routine routine = response.routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        response = routineService.kickStepUp(routine.getDescendants().stream()
                        .filter(step -> step.name().equals(TWOTWOTWO))
                        .findFirst()
                        .get()
                        .id(),
                LocalDateTime.now());

        assertTrue(response.success());
        routine = response.routine();

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTWOTWO,
                        TWOTHREE),
                routine);

        List<RoutineStep> twoTwoTwoList = routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWOTWO))
                .toList();

        assertEquals(TimeableStatus.EXCLUDED, twoTwoTwoList.get(0).status());
        assertEquals(TimeableStatus.NOT_STARTED, twoTwoTwoList.get(1).status());

        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        assertEquals(3, queryService.fetchChildCount(twoTwo.childId(), null));

        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwoTwo.recurring(false))));
    }

    @Test
    @Transactional
    void testKickUpStep_ToRoot() {
        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwo.recurring(false))));

        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        RoutineResponse response = routineService.createRoutine(root.id(), null);

        assertTrue(response.success());
        Routine routine = response.routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        response = routineService.kickStepUp(routine.getDescendants().stream()
                        .filter(step -> step.name().equals(TWOTWO))
                        .findFirst()
                        .get()
                        .id(),
                LocalDateTime.now());

        assertTrue(response.success());
        routine = response.routine();

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        List<RoutineStep> twoTwoList = routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWO))
                .toList();

        assertEquals(TimeableStatus.EXCLUDED, twoTwoList.get(0).status());
        assertEquals(TimeableStatus.NOT_STARTED, twoTwoList.get(1).status());

        assertEquals(TimeableStatus.EXCLUDED, routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWOONE))
                .findFirst()
                .get()
                .status());

        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwo.recurring(true))));
    }

    private Routine pushStepTest() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        RoutineResponse response = routineService.createRoutine(root.id(), null);

        assertTrue(response.success());
        Routine routine = response.routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        response = routineService.pushStepForward(routine.getDescendants().stream()
                        .filter(step -> step.name().equals(TWOTWOTWO))
                        .findFirst()
                        .get()
                        .id(),
                LocalDateTime.now());

        assertTrue(response.success());
        routine = response.routine();

        return routine;
    }

    @Test
    @Transactional
    void testPushStep() { // Not recurring
        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTWOTWO,
                        TWOTHREE),
                pushStepTest());

        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        assertEquals(3, queryService.fetchChildCount(twoTwo.childId(), null));
    }

    @Test
    @Transactional
    void testPushStep_Recurring() {
        TaskNode twoTwoTwo = nodes.get(Triple.of(TWOTWO, TWOTWOTWO, 1));
        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwoTwo.recurring(true))));

        Routine routine = pushStepTest();

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTWOTWO,
                        TWOTHREE),
                routine);

        List<RoutineStep> twoTwoTwoList = routine.getDescendants().stream()
                .filter(step -> step.name().equals(TWOTWOTWO))
                .toList();

        assertEquals(TimeableStatus.EXCLUDED, twoTwoTwoList.get(0).status());
        assertEquals(TimeableStatus.NOT_STARTED, twoTwoTwoList.get(1).status());

        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));
        assertEquals(4, queryService.fetchChildCount(twoTwo.childId(), null));

        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwoTwo.recurring(false))));
    }

    @Test
    @Transactional
    @Disabled
    void testPushStep_AsRoot() {
        // TODO: implement
    }
}
