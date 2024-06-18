package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.RoutineTestTemplateWithRequiredTasks;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceRefreshTest extends RoutineTestTemplateWithRequiredTasks {

    @BeforeAll
    void setup() {
        init();
        routineService.refreshRoutines(true);
    }

    private LocalDateTime prepareRoutineRefresh(LocalTime customTime) {
        LocalDateTime startTime = customTime.atDate(clock.time().toLocalDate());
        clock.manualTime(startTime);

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.of(LocalTime.from(startTime.plus(
                        Duration.ofMinutes(14*60 + 30)))))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        LocalTime etaTime = customTime.plus(Duration.ofMinutes(14*60 + 30));
        assertEquals(etaTime, queryService.fetchNode(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2)).id())
                .projectEtaLimit().get());

        return startTime;
    }

    private void assertRoutineRefreshEarly(LocalDateTime startTime, RoutineLimitFilter filter) {
        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(14*60 + 30),
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of());

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(THREETHREE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of(
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plusHours(14));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of(
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        clock.manualTime(startTime
                .plus(tasks.get(THREETHREE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of(THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = iterateCompleteStep(routine, 2,
                THREETWO, TimeableStatus.ACTIVE,
                THREEONE, TimeableStatus.COMPLETED);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));
    }

    private void assertRoutineRefreshLate(LocalDateTime startTime, RoutineLimitFilter filter) {
        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(3*60 + 30),
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE));

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(THREETHREE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plusHours(14));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plusHours(2));
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.minusHours(12));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        // Cleanup

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));
    }

    @Test
    void testDynamicRoutineRefresh_Early() {
        LocalTime startTime = LocalTime.of(5, 0);
        assertRoutineRefreshEarly(prepareRoutineRefresh(startTime), null);
    }

    @Test
    void testDynamicRoutineRefresh_Late() {
        LocalTime startTime = LocalTime.of(23, 0);
        assertRoutineRefreshLate(prepareRoutineRefresh(startTime), null);
    }

    private RoutineLimitFilter getCustomFilter(LocalTime customTime) {
        return new RoutineLimitFilter()
                .etaLimit(LocalDateTime.of(clock.time().toLocalDate(),
                        customTime.plus(Duration.ofMinutes(14*60 + 30))));
    }

    @Test
    void testDynamicRoutineRefreshCustomFilter_Early() {
        LocalTime startTime = LocalTime.of(5, 0);
        assertRoutineRefreshEarly(prepareRoutineRefresh(startTime), getCustomFilter(startTime));
    }

    @Test
    void testDynamicRoutineRefreshCustomFilter_Late() {
        LocalTime startTime = LocalTime.of(23, 0);
        assertRoutineRefreshLate(prepareRoutineRefresh(startTime), getCustomFilter(startTime));
    }

    @Test
    void testDynamicRoutineRefresh_NestedEta_Early() {
        testDynamicRoutineRefresh_NestedEta_Early(LocalTime.of(5, 0));
    }

    @Test
    void testDynamicRoutineRefresh_NestedEta_Late() {
        testDynamicRoutineRefresh_NestedEta_Late(LocalTime.of(23, 0));
    }

    private void testDynamicRoutineRefresh_NestedEta_Early(LocalTime customTime) {
        LocalDateTime startTime = customTime.atDate(clock.time().toLocalDate());
        clock.manualTime(startTime);

        RoutineLimitFilter filter = null;

        TaskNode threeTwo = nodes.get(Triple.of(THREE_AND_FIVE, THREETWO, 1));
        LocalDateTime nestedEta = startTime.plus(
                netDurationService.getNetDuration(threeTwo.id(), null));

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.of(LocalTime.from(startTime.plus(
                        Duration.ofMinutes(14 * 60 + 30)))))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        changeService.execute(new MergeChange<>(threeTwo
                .projectEtaLimit(Optional.of(LocalTime.from(nestedEta)))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        testDynamicRoutineRefresh_NestedEta_Early_PartOne(nestedEta, startTime, filter);
        clock.manualTime(startTime);
        testDynamicRoutineRefresh_NestedEta_Early_PartTwo(nestedEta, startTime, filter);

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        changeService.execute(new MergeChange<>(threeTwo
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));
    }

    private void testDynamicRoutineRefresh_NestedEta_Early_PartOne(LocalDateTime nestedEta, LocalDateTime startTime, RoutineLimitFilter filter) {
        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(14 * 60 + 30),
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of());

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(SIX_AND_THREETWOFOUR).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETHREE),
                List.of(
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        clock.manualTime(startTime.plusHours(14));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(nestedEta);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        clock.manualTime(nestedEta);
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(nestedEta);
        routine = routineService.refreshRoutine(routine.id());

        routine = iterateCompleteStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREEONE, TimeableStatus.COMPLETED);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 2,
                THREETWO, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETHREE),
                List.of(
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        routine = iterateStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREETWO, TimeableStatus.EXCLUDED,
                routineService::excludeStep);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        assertEquals(TimeableStatus.EXCLUDED, routine.steps().values()
                .stream()
                .filter(step -> step.name().equals(THREETWO))
                .findFirst().get().status());
    }

    void testDynamicRoutineRefresh_NestedEta_Early_PartTwo(LocalDateTime nestedEta, LocalDateTime startTime, RoutineLimitFilter filter) {
        changeService.execute(new MergeChange<>(nodes.get(Triple.of(THREE_AND_FIVE, THREEONE, 0))
                .completed(false)));

        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(14 * 60 + 30),
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of());

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                List.of(),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        routine = iterateCompleteStep(routine, 2,
                THREETWO, TimeableStatus.ACTIVE,
                THREEONE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 3,
                THREETWOONE_AND_THREETWOTHREE, TimeableStatus.ACTIVE,
                THREETWO, TimeableStatus.DESCENDANT_ACTIVE);

        routine = iterateCompleteStep(routine, 4,
                TWOTWOTHREE_AND_THREETWOTWO, TimeableStatus.ACTIVE,
                THREETWOONE_AND_THREETWOTHREE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 5,
                THREETWOONE_AND_THREETWOTHREE, TimeableStatus.ACTIVE,
                TWOTWOTHREE_AND_THREETWOTWO, TimeableStatus.COMPLETED);

        clock.manualTime(nestedEta.minusMinutes(30));
        routine = routineService.refreshRoutine(routine.id());

        assertEquals(routine.currentStep().name(), THREETWOONE_AND_THREETWOTHREE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE),
                List.of(
                        THREETHREE,
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 2,
                THREETWO, TimeableStatus.ACTIVE,
                THREETWOONE_AND_THREETWOTHREE, TimeableStatus.COMPLETED);

        assertEquals(TimeableStatus.LIMIT_EXCEEDED,
                routine.steps().values().stream()
                        .filter(step -> step.name().equals(THREETHREE))
                        .findFirst().get().status());

        routine = iterateCompleteStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREETWO, TimeableStatus.COMPLETED);

        // TODO: Shouldn't this go directly to ThreeThree, not up to ThreeAndFive?

        assertEquals(TimeableStatus.NOT_STARTED,
                routine.steps().values().stream()
                        .filter(step -> step.name().equals(THREETHREE))
                        .findFirst().get().status());

        assertEquals(routine.descendants().size() - 2,
                routine.steps().values()
                        .stream().filter(step -> step.status().isFinishedOrExceeded())
                        .count());

        assertFalse(tasks.get(THREETHREE).required());
        assertTrue(tasks.get(THREE_AND_FIVE).project());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETHREE),
                List.of(
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 7,
                THREETHREE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETHREE),
                List.of(
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREETHREE, TimeableStatus.COMPLETED);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        THREETHREE),
                List.of(
                        SIX_AND_THREETWOFOUR),
                routine,
                filter);

        int position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                clock.time(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED);

        RoutineStep previousStep = routine.descendants().get(position);
        assertRoutineStep(
                previousStep,
                THREE_AND_FIVE,
                TimeableStatus.COMPLETED);
    }

    private void testDynamicRoutineRefresh_NestedEta_Late(LocalTime customTime) {
        LocalDateTime startTime = customTime.atDate(clock.time().toLocalDate());
        clock.manualTime(startTime);

        RoutineLimitFilter filter = null;

        TaskNode threeTwo = nodes.get(Triple.of(THREE_AND_FIVE, THREETWO, 1));
        LocalDateTime nestedEta = startTime.plus(
                netDurationService.getNetDuration(threeTwo.id(), null));

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.of(LocalTime.from(startTime.plus(
                        Duration.ofMinutes(14*60 + 30)))))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        changeService.execute(new MergeChange<>(threeTwo
                .projectEtaLimit(Optional.of(LocalTime.from(nestedEta)))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(3*60 + 30),
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE));

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plus(tasks.get(SIX_AND_THREETWOFOUR).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime.plusHours(14));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(nestedEta);
//                Stream.of(
//                        THREETWO,
//                                THREETWOONE_AND_THREETWOTHREE,
//                                TWOTWOTHREE_AND_THREETWOTWO,
//                                THREETWOONE_AND_THREETWOTHREE)
//                        .map(tasks::get)
//                        .map(Task::duration)
//                        .reduce(Duration.ZERO, Duration::plus)));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(nestedEta);
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        routine = iterateCompleteStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREEONE, TimeableStatus.COMPLETED);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        clock.manualTime(startTime);
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                filter);

        // Cleanup

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));

        changeService.execute(new MergeChange<>(threeTwo
                .projectEtaLimit(Optional.empty())
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));
    }

    // TODO:
    // - Test for auto-cycle to bottom
    // - Test for NESTED eta's

    @Test
    void testDynamicRoutineRefreshAcrossMultipleDays_NonOptimistic() {
        LocalTime customTime = LocalTime.of(23, 0);

        LocalDateTime startTime = customTime.atDate(clock.time().toLocalDate());
        clock.manualTime(startTime);

        changeService.execute(new MergeChange<>(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2))
                .projectEtaLimit(Optional.of(LocalTime.from(startTime.plus(
                        Duration.ofMinutes(14*60 + 30)))))
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())));


        LocalTime etaTime = customTime.plus(Duration.ofMinutes(14*60 + 30));
        assertEquals(etaTime, queryService.fetchNode(nodes.get(Triple.of(NULL, THREE_AND_FIVE, 2)).id())
                .projectEtaLimit().get());

        Routine routine = linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(3*60 + 30),
                null,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE));

        routine = routineService.refreshRoutine(routine.id());
        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                null);


        clock.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                null);

        clock.manualTime(startTime.plusHours(1));
        routine = routineService.refreshRoutine(routine.id());

        assertRoutineWithExceeded(
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                null);

        clock.manualTime(startTime);
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.DESCENDANT_ACTIVE);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                null);

        routine = iterateCompleteStep(routine, 0,
                THREE_AND_FIVE, TimeableStatus.ACTIVE,
                THREEONE, TimeableStatus.COMPLETED);

        assertRoutineWithExceeded(List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE),
                routine,
                null);
    }
}
