package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceWithRequiredTest extends RoutineTestTemplateWithRequiredTasks {

    @BeforeAll
    void setup() {
        init();
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitedProject() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                id -> Duration.ofHours(2),
                List.of(
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));
    }

    @Test
    void testJumpToStepInRoutine() throws Exception {
        Routine routine = linkRoutineCreationTest(
                Triple.of(NULL, TWO, 1),
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));

        routine = routineService.startStep(routine.currentStep().id(), LocalDateTime.now()).routine();

        RoutineStep twoTwo = routine.steps().values()
                .stream()
                .filter(step -> step.name().equals(TWOTWO))
                .findFirst()
                .orElseThrow();

        RoutineResponse response = routineService.jumpToStep(twoTwo.id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        twoTwo = routine.steps().values()
                .stream()
                .filter(step -> step.name().equals(TWOTWO))
                .findFirst()
                .orElseThrow();

        RoutineStep two = routine.steps().values()
                .stream()
                .filter(step -> step.name().equals(TWO))
                .findFirst()
                .orElseThrow();

        assertEquals(twoTwo, routine.currentStep());
        assertEquals(TimeableStatus.NOT_STARTED, routine.currentStep().status());
        assertEquals(TimeableStatus.SKIPPED, two.status());
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingStepCountViaFilter() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.empty()))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .stepCountLimit(3);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOTWO,
                        TWOTWOONE));

        filter = new RoutineLimitFilter()
                .stepCountLimit(0);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                filter,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));

        filter = new RoutineLimitFilter()
                .stepCountLimit(1);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));

        filter = new RoutineLimitFilter()
                .stepCountLimit(2);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));

        filter = new RoutineLimitFilter()
                .stepCountLimit(99);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingStepCountViaLink() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.empty())
                        .projectStepCountLimit(Optional.of(3)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                node.projectStepCountLimit(Optional.of(0)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                null,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));

        changeService.execute(Request.of(new MergeChange<>(
                node.projectStepCountLimit(Optional.of(1)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));

        changeService.execute(Request.of(new MergeChange<>(
                node.projectStepCountLimit(Optional.of(2)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                node.projectStepCountLimit(Optional.of(99)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingEtaViaFilter() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                root.projectDurationLimit(Optional.empty()))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(routineDuration
                        .plusSeconds(1)));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));

        filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(routineDuration
                        .minusHours(2)
                        .plusSeconds(1)));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                filter,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));

        filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.MAX);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    private void createRoutineFromProjectLinkWithLimitingEtaViaLink(LocalDateTime startTime) {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(new MergeChange<>(root
                .projectDurationLimit(Optional.empty())
                .projectEtaLimit(Optional.of(LocalTime.from(startTime
                        .plus(routineDuration)
                        .plusSeconds(1))))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));

        changeService.execute(Request.of(new MergeChange<>(
                root.projectEtaLimit(Optional.of(LocalTime.from(startTime
                        .plus(routineDuration
                                .minusHours(3)
                                .plusSeconds(1))))))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                null,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));

        changeService.execute(Request.of(new MergeChange<>(
                root.projectEtaLimit(Optional.of(LocalTime.from(startTime
                        .plusSeconds(1)))))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                null,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE,
                        TWOTWOONE,
                        TWOTWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                root.projectEtaLimit(Optional.empty()))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));

        routineService.manualTime(null);
    }

    @Test
    @Transactional
    void testCreateRoutineFromProjectLinkWithLimitingEtaViaLink_SingleDay() {
        LocalDateTime startTime = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.of(8, 0));
        routineService.manualTime(startTime);

        createRoutineFromProjectLinkWithLimitingEtaViaLink(startTime);
    }

    @Test
    @Transactional
    @Disabled
    void testCreateRoutineFromProjectLinkWithLimitingEtaViaLink_MultipleDays() {
        LocalDateTime startTime = LocalDateTime.of(
                LocalDate.now(),
                LocalTime.of(20, 0));
        routineService.manualTime(startTime);

        createRoutineFromProjectLinkWithLimitingEtaViaLink(startTime);
    }

    @Test
    @Disabled
    void testDynamicETALimitStepInsertion() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                root.projectDurationLimit(Optional.of(routineDuration)))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(routineDuration.plusHours(2)));

        Routine routine = routineService.createRoutine(root.id(), filter, routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE),
                routine);

        assertRoutineAll(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWO,
                TimeableStatus.ACTIVE);

        changeService.execute(Request.of(new MergeChange<>(
                root.projectDurationLimit(Optional.of(routineDuration
                        .plusHours(2))))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWO,
                        TWOONE),
                routine);

        assertRoutineAll(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        routineService.refreshRoutines(true);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineAll(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOONE,
                TimeableStatus.COMPLETED);

        routineService.refreshRoutines(false);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineAll(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.ACTIVE);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO),
                routine);

        assertRoutineStepExecution(
                routine,
                4,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.COMPLETED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineStepExecution(
                routine,
                4,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.COMPLETED);
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitingStepCount() {
        TaskNode two = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));

        changeService.execute(Request.of(
                new MergeChange<>(two
                        .projectDurationLimit(Optional.empty())
                        .projectEtaLimit(Optional.empty())),
                new MergeChange<>(twoTwo
                        .projectStepCountLimit(Optional.of(3))
                        .projectDurationLimit(Optional.empty()))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo.projectStepCountLimit(Optional.of(1)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6).plus(
                        Duration.ofMinutes(30)),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo.projectStepCountLimit(Optional.of(99)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        changeService.execute(Request.of(
                new MergeChange<>(
                        twoTwo.projectStepCountLimit(Optional.of(2))),
                new MergeChange<>(
                        tasks.get(TWOTWOTHREE_AND_THREETWOTWO)
                                .required(false))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(TWOTWOTHREE_AND_THREETWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo.projectStepCountLimit(Optional.of(1)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5).plus(
                        Duration.ofMinutes(30)),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTHREE),
                List.of(
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    @Disabled
    void testDynamicETALimitStepExclusionFromParent() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                root.projectDurationLimit(Optional.of(routineDuration)))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(routineDuration.plusHours(1)));

        Routine routine = routineService.createRoutine(root.id(), filter, routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        routineService.manualTime(LocalDateTime.now().plus(Duration.ofHours(7)));

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWO,
                TimeableStatus.ACTIVE);

        assertRoutine(List.of(
                        TWO,
                        TWOONE),
                routine);

        routineService.manualTime(null);
    }

    @Test
    @Disabled
    void testDynamicETALimitStepExclusionFromSibling() {
        TaskNode root = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                root.projectDurationLimit(Optional.of(routineDuration)))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(routineDuration.plusHours(1)));

        Routine routine = routineService.createRoutine(root.id(), filter, routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWO,
                TimeableStatus.ACTIVE);

        LocalDateTime manualTime = LocalDateTime.now().plus(Duration.ofHours(12));
        routineService.manualTime(manualTime);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                manualTime,
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineStep(
                previous,
                TWOONE,
                TimeableStatus.COMPLETED);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStepExecution(
                routine,
                2,
                TWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        routineService.manualTime(null);
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_2() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(Duration.ofHours(3))))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_3() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_4() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        RoutineLimitFilter filter = new RoutineLimitFilter();
        filter.durationLimit(routineDuration);

        taskRoutineCreationTestWithExpectedDurationAndFilter(
                TWO,
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        RoutineLimitFilter filter = new RoutineLimitFilter();
        filter.durationLimit(routineDuration);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTHREE,
                        TWOTWOTWO,
                        TWOTWOONE));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_TWO() throws Exception {
        TaskNode two = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(
                new MergeChange<>(
                        two.projectDurationLimit(Optional.of(routineDuration)))));

        two = queryService.fetchNode(two.id());
        assertEquals(routineDuration, two.projectDurationLimit().get());

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(13*60 + 30),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of(
                        THREETHREE));
    }

    @Test
    void testCreateRoutineFromDifferentProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 4),
                id -> Duration.ofMinutes(210),
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
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitedProjectWithTagFilter() throws Exception {
        Change newTagChange = new PersistChange<>(new Tag().name("TestTag"));
        Tag newTag = (Tag) changeService.execute(Request.of(newTagChange)).changeRelevantDataMap()
                .get(newTagChange.id()).get(0);

        List<TaskID> targetTasks = Stream.of(tasks.get(TWOTWO), tasks.get(TWOTWOONE))
                .map(Task::id)
                .toList();

        Change multiTagChange = new MultiMergeChange<>(new Task()
                .tags(Set.of(newTag)),
                targetTasks);

        changeService.execute(Request.of(multiTagChange));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                new TaskNodeTreeFilter()
                        .includedTagIds(Set.of(newTag.id())),
                id -> Duration.ofMinutes(90),
                List.of(
                        TWOTWO,
                        TWOTWOONE),
                List.of());
    }

    @Test
    void testRoutineRecalculateWithPersistAtBack() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id()))));

        routine = routineService.fetchRoutine(routine.id());

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREEONE),
                routine);

        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
    }

    @Test
    void testRoutineRecalculateWithPersistAtFront() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        LocalDateTime mark = LocalDateTime.now();

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                mark,
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.ACTIVE);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                mark,
                routineService::suspendStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.SUSPENDED,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.SUSPENDED);

        changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id())
                        .position(0))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        THREEONE,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));

        assertRoutineStepExecution(
                routine,
                1,
                THREEONE,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.SUSPENDED);
    }

    @Test
    void testRoutineRecalculateWithDelete() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.ACTIVE);


        changeService.execute(Request.of(new DeleteChange<>(
                ID.of(links.get(Triple.of(TWOTWO, TWOTWOONE, 0))))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertEquals(originalDuration.minus(Duration.ofMinutes(30)), routine.estimatedDuration());
    }

    @Test
    void testRoutineRecalculateWithDeleteWithChildren() {
        Routine routine = routineService.createRoutine(tasks.get(TWO).id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                routine);

        assertNotNull(routine.currentStep().children().get(1).node());

        Duration originalDuration = routine.estimatedDuration();
        Duration removedDuration = Duration.ZERO;

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWO,
                TimeableStatus.ACTIVE);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);
        removedDuration = removedDuration.plus(previous.duration());

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOONE,
                TimeableStatus.COMPLETED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);
        removedDuration = removedDuration.plus(previous.duration());

        assertRoutineStepExecution(
                routine,
                5,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.ACTIVE);

        removedDuration = removedDuration.plus(routine.currentStep().duration());
        changeService.execute(Request.of(new DeleteChange<>(
                ID.of(links.get(Triple.of(TWO, TWOTWO, 1))))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWO,
                        TWOONE,
                        TWOTHREE),
                routine);

        assertRoutineStepExecution(
                routine,
                1,
                TWOONE,
                TimeableStatus.COMPLETED,
                TimeableStatus.ACTIVE);

        assertEquals(originalDuration.minus(removedDuration), routine.estimatedDuration());
    }

    @Test
    void testRoutineRecalculateWithMerge() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        Task twoTwoOne = tasks.get(TWOTWOONE);
        changeService.execute(Request.of(new MergeChange<>(
                new Task(twoTwoOne.id())
                        .duration(twoTwoOne.duration().plus(Duration.ofHours(1))))));

        routine = routineService.fetchRoutine(routine.id());

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(Duration.ofHours(4), routine.estimatedDuration());
    }

    @Test
    @Transactional
    void testCycleToEnd() {
        Task root = tasks.get(TWOTWO);
        TaskNode target = nodes.get(Triple.of(TWOTWO, TWOTWOONE, 0));

        changeService.execute(Request.of(new MergeChange<>(
                target
                        .cycleToEnd(true)
                        .recurring(true))));

        Routine routine = routineService.createRoutine(root.id(), routineService.now()).routine();

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.ACTIVE);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.COMPLETED);

        target = queryService.fetchNode(target.id());
        List<String> siblings = queryService.fetchChildNodes(target.parentId())
                .map(TaskNode::name)
                .toList();

        assertEquals(List.of(
                    TWOTWOTWO,
                    TWOTWOONE,
                    TWOTWOTHREE_AND_THREETWOTWO),
                siblings);
    }
}
