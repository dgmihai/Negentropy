package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.RoutineTestTemplateWithRequiredTasks;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceRefreshTest extends RoutineTestTemplateWithRequiredTasks {

    @BeforeAll
    void setup() {
        init();
        routineService.refreshRoutines(true);
    }

    @Test
    void testDynamicRoutineRefreshInOneDay() {
        LocalTime customTime = LocalTime.of(5, 0);
        LocalDateTime startTime = customTime.atDate(routineService.now().toLocalDate());
        routineService.manualTime(startTime);

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
                id -> Duration.ofMinutes(14*60 + 30),
                null,
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
                null);

        routineService.manualTime(startTime.plus(tasks.get(THREETHREE).duration()));
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
                null);

        routineService.manualTime(startTime.plusHours(14));
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
                null);

        routineService.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
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
                null);

        routineService.manualTime(startTime);
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
                null);

        routineService.manualTime(startTime.plusHours(2));
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.ACTIVE);

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

        routineService.manualTime(startTime);
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
                null);

        routineService.manualTime(startTime);
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
                null);
    }

    @Test
    @Disabled
    void testDynamicRoutineRefreshAcrossMultipleDays_Optimistic() {
        LocalTime customTime = LocalTime.of(23, 0);

        LocalDateTime startTime = customTime.atDate(routineService.now().toLocalDate());
        routineService.manualTime(startTime);

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
                id -> Duration.ofMinutes(14*60 + 30),
                null,
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
                null);

        routineService.manualTime(startTime.plus(tasks.get(THREETHREE).duration()));
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
                null);

        routineService.manualTime(startTime.plusHours(14));
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
                null);

        routineService.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
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
                null);

        routineService.manualTime(startTime);
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
                null);

        routineService.manualTime(startTime.plusHours(2));
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.ACTIVE);

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

        routineService.manualTime(startTime);
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
                null);

        routineService.manualTime(startTime);
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
                null);
    }

    @Test
    void testDynamicRoutineRefreshAcrossMultipleDays_NonOptimistic() {
        LocalTime customTime = LocalTime.of(0, 0).minus(
                netDurationService.getNetDuration(
                        nodes.get(Triple.of(THREE_AND_FIVE, THREETWO, 1)).id(), null)
                        .plus(Duration.ofHours(1)));

        LocalDateTime startTime = customTime.atDate(routineService.now().toLocalDate());
        routineService.manualTime(startTime);

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


        routineService.manualTime(startTime.plus(tasks.get(THREEONE).duration()));
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

        routineService.manualTime(startTime.plusHours(1));
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

        routineService.manualTime(startTime);
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.ACTIVE);

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

//        routine = iterateCompleteStep(routine, 2,
//                THREETWO, TimeableStatus.ACTIVE,
//                THREEONE, TimeableStatus.COMPLETED);
//
//        assertRoutineWithExceeded(List.of(
//                        THREE_AND_FIVE,
//                        THREEONE),
//                List.of(
//                        THREETWO,
//                        THREETWOONE_AND_THREETWOTHREE,
//                        TWOTWOTHREE_AND_THREETWOTWO,
//                        THREETWOONE_AND_THREETWOTHREE,
//                        SIX_AND_THREETWOFOUR,
//                        THREETHREE),
//                routine,
//                null);
    }

    @Test
    void testDynamicRoutineRefreshCustomFilter() {
        LocalDateTime startTime = routineService.now();

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(startTime.plus(
                        Duration.ofHours(15)));

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

        routineService.manualTime(startTime.plus(tasks.get(THREETHREE).duration()));
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

        routineService.manualTime(startTime.plusHours(14));
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

        routineService.manualTime(startTime);
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

        routineService.manualTime(startTime.plusHours(2));
        routine = iterateCompleteStep(routine, 1,
                THREEONE, TimeableStatus.ACTIVE,
                THREE_AND_FIVE, TimeableStatus.ACTIVE);

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

        routineService.manualTime(startTime);
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
    }
}
