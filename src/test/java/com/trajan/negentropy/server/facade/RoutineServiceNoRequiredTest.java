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
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.RoutineTestTemplate;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.util.ServerClockService;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceNoRequiredTest extends RoutineTestTemplate {

    @BeforeAll
    void setup() {
        init();
    }

    @Test
    void testCreateRoutineFromTask() {
        taskRoutineCreationTest(
                THREETWO,
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR));
    }

    @Test
    void testCreateRoutineFromLink() throws Exception {
        linkRoutineCreationTest(
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectTask() {
        taskRoutineCreationTest(
                TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitedProject() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                id -> Duration.ofHours(2),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO),
                List.of(
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_1() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(2);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.of(routineDuration)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO),
                List.of(
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));
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
                node.projectDurationLimit(Optional.of(Duration.ofHours(5))))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO),
                List.of(
                        TWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO));
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
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(TWOTWOTHREE_AND_THREETWOTWO));
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
                        TWOTWOONE,
                        TWOTWOTWO));
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
                        TWOTWOONE,
                        TWOTWOTWO),
                List.of(
                        TWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_TWO() throws Exception {
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
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_THREE_AND_FIVE() {
        taskRoutineCreationTest(
                THREE_AND_FIVE,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithManualNonLimitingProjectDuration_THREE_AND_FIVE() {
        RoutineLimitFilter filter = new RoutineLimitFilter();
        Duration routineDuration = Duration.ofMinutes(14*60 + 30);
        filter.durationLimit(routineDuration);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> routineDuration,
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
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualNonLimitingProjectDuration_THREE_AND_FIVE() {
        RoutineLimitFilter filter = new RoutineLimitFilter();
        Duration routineDuration = Duration.ofMinutes(14*60 + 30);
        filter.durationLimit(routineDuration);

        taskRoutineCreationTestWithExpectedDurationAndFilter(
                THREE_AND_FIVE,
                id -> routineDuration,
                filter,
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR,
                        THREETHREE));
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
                new TaskNodeTreeFilter().includedTagIds(Set.of(newTag.id())),
                id -> Duration.ofMinutes(90),
                List.of(
                        TWOTWO,
                        TWOTWOONE),
                List.of());
    }
    @Test
    void testExecuteRoutineAndGoPreviousImmediately() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.children().get(routine.currentPosition()).task().name());
        assertEquals(TimeableStatus.NOT_STARTED, routine.currentStep().status());
        assertEquals(TimeableStatus.NOT_STARTED, routine.status());
        assertNull(routine.currentStep().parentId());

        LocalDateTime time1 = ServerClockService.now();
        doRoutineFalse(routine.currentStep().id(),
                time1,
                routineService::previousStep);
    }

    @Test
    void testExecuteOnRoutineStepWhichIsNotCurrentOrParentOfCurrent() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);

        RoutineStep twoTwoTwo = routine.currentStep().children().get(1);

        assertEquals(twoTwoTwo.task().id(), tasks.get(TWOTWOTWO).id());

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        routine = doRoutine(twoTwoTwo.id(),
                LocalDateTime.now(),
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);
    }

    @Test
    void testExecuteRoutine() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);

        assertNull(routine.currentStep().parentId());

        LocalDateTime time1 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time1,
                routineService::startStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        int position = routine.currentPosition();
        LocalDateTime time2 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time2,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time2);

        RoutineStep stepTwoTwo = routine.descendants().get(position);
        assertRoutineStepParent(
                routine,
                stepTwoTwo,
                TWOTWO,
                TimeableStatus.DESCENDANT_ACTIVE);

        assertEquals(
                Duration.between(time1, time2),
                timeableUtil.getElapsedActiveDuration(stepTwoTwo, time2));

        LocalDateTime now = LocalDateTime.now();
        assertEquals(
                Duration.between(time2, now),
                timeableUtil.getElapsedActiveDuration(routine.currentStep(), now));

        position = routine.currentPosition();
        LocalDateTime time3 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time3,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time3);

        RoutineStep stepTwoTwoOne = routine.descendants().get(position);
        assertRoutineStep(
                stepTwoTwoOne,
                TWOTWOONE,
                TimeableStatus.COMPLETED);

        assertEquals(0, stepTwoTwoOne.children().size());
        assertEquals(time3, stepTwoTwoOne.finishTime());
        assertEquals(time3, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time2, time3),
                timeableUtil.getElapsedActiveDuration(stepTwoTwoOne, time3));

        LocalDateTime time4 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time4,
                routineService::suspendStep);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.SUSPENDED,
                TimeableStatus.ACTIVE,
                time3);

        assertRoutineStep(
                stepTwoTwoOne,
                TWOTWOONE,
                TimeableStatus.COMPLETED);

        assertEquals(time4, routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time3, time4),
                timeableUtil.getElapsedActiveDuration(routine.currentStep(), time4));

        LocalDateTime time5 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time5,
                routineService::startStep);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time3);

        assertEquals(Duration.between(time4, time5), routine.currentStep().elapsedSuspendedDuration());
        assertNull(routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time4, time5),
                routine.currentStep().elapsedSuspendedDuration());
        assertEquals(
                Duration.between(time3, time5).minus(routine.currentStep().elapsedSuspendedDuration()),
                timeableUtil.getElapsedActiveDuration(routine.currentStep(), time5));

        position = routine.currentPosition();
        LocalDateTime time6 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time6,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time6);

        RoutineStep stepTwoTwoTwo = routine.descendants().get(position);
        assertRoutineStep(
                stepTwoTwoTwo,
                TWOTWOTWO,
                TimeableStatus.COMPLETED);

        assertEquals(time6, stepTwoTwoTwo.finishTime());

        // TODO: Failure here, occasionally
//        assertEquals(
//                Duration.between(time5, time6),
//                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time6));

        position = routine.currentPosition();
        LocalDateTime time7 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time7,
                routineService::previousStep);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.COMPLETED,
                TimeableStatus.ACTIVE,
                time3);

        RoutineStep stepTwoTwoThree = routine.descendants().get(position);
        assertRoutineStep(
                stepTwoTwoThree,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.SKIPPED);

//        assertEquals(
//                Duration.between(time3, time7).minus(routine.currentStep().elapsedSuspendedDuration()),
//                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time7));

        position = routine.currentPosition();
        LocalDateTime time8 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time8,
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time6);

        stepTwoTwoTwo = routine.descendants().get(position);
        assertRoutineStep(
                stepTwoTwoTwo,
                TWOTWOTWO,
                TimeableStatus.SKIPPED);

        assertEquals(
                Duration.between(time7, time8),
                routine.currentStep().elapsedSuspendedDuration());

        position = routine.currentPosition();
        LocalDateTime time9 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time9,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        stepTwoTwoThree = routine.descendants().get(position);
        assertEquals(time9, stepTwoTwoThree.finishTime());
        assertRoutineStep(
                stepTwoTwoThree,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED);

        assertRoutineStep(
                stepTwoTwoTwo,
                TWOTWOTWO,
                TimeableStatus.SKIPPED);

        position = routine.currentPosition();
        LocalDateTime time10 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time10,
                routineService::completeStep);

        stepTwoTwo = routine.descendants().get(position);
        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time3);

        assertRoutineStep(
                stepTwoTwo,
                TWOTWO,
                TimeableStatus.DESCENDANT_ACTIVE);

        position = routine.currentPosition();
        LocalDateTime time11 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time11,
                routineService::postponeStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        stepTwoTwoTwo = routine.descendants().get(position);
        assertRoutineStep(
                stepTwoTwoTwo,
                TWOTWOTWO,
                TimeableStatus.POSTPONED);

        position = routine.currentPosition();
        LocalDateTime time12 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time12,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED,
                time1);

        assertEquals(time12, routine.finishTime());
    }

    @Test
    void testExecuteRoutineTwo() {
        TaskID rootId = tasks.get(THREE_AND_FIVE).id();

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);
        assertNull(routine.currentStep().parentId());

        int position = routine.currentPosition();
        LocalDateTime time0 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time0,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                1,
                THREEONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time0);

        RoutineStep stepThreeAndFive = routine.descendants().get(position);
        assertRoutineStep(
                stepThreeAndFive,
                THREE_AND_FIVE,
                TimeableStatus.DESCENDANT_ACTIVE);

        position = routine.currentPosition();
        LocalDateTime time1 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time1,
                routineService::postponeStep);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        RoutineStep stepThreeOne = routine.descendants().get(position);
        assertRoutineStep(
                stepThreeOne,
                THREEONE,
                TimeableStatus.POSTPONED);

        position = routine.currentPosition();
        LocalDateTime time2 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time2,
                routineService::previousStep);

        assertRoutineStepExecution(
                routine,
                1,
                THREEONE,
                TimeableStatus.POSTPONED,
                TimeableStatus.ACTIVE,
                time0);

        RoutineStep stepThreeTwo = routine.descendants().get(position);
        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.SKIPPED);

        position = routine.currentPosition();
        LocalDateTime time3 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time3,
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        stepThreeOne = routine.descendants().get(position);
        assertRoutineStep(
                stepThreeOne,
                THREEONE,
                TimeableStatus.POSTPONED);

        LocalDateTime time4 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time4,
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                7,
                THREETHREE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time4);

        stepThreeTwo = routine.descendants().get(position);
        for (RoutineStep step : stepThreeTwo.children()) {
            assertEquals(TimeableStatus.SKIPPED, step.status());
        }

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time0);

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        position = routine.currentPosition();
        LocalDateTime time5 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time5,
                routineService::completeStep);
        stepThreeTwo = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                3,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time5);

        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.DESCENDANT_ACTIVE);

        // Exclude ThreeTwoOne
        LocalDateTime time6 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time6,
                routineService::excludeStep);
        RoutineStep stepThreeTwoOne = routine.descendants().get(3);
        RoutineStep stepThreeTwoThree = routine.descendants().get(5);
        RoutineStep stepThreeTwoFour = routine.descendants().get(6);

        assertRoutineStepExecution(
                routine,
                4,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time6);

        assertRoutineStep(
                stepThreeTwoOne,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.EXCLUDED);

        assertRoutineStep(
                stepThreeTwoThree,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.NOT_STARTED);

        assertRoutineStep(
                stepThreeTwoFour,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.NOT_STARTED);

        // Jump to ThreeTwo
        LocalDateTime time7 = LocalDateTime.now();
        routine = doRoutine(stepThreeTwo.id(),
                time7,
                routineService::jumpToStepAndStartIfReady);
        stepThreeTwoOne = routine.descendants().get(3);
        RoutineStep stepThreeTwoTwo = routine.descendants().get(4);
        stepThreeTwoThree = routine.descendants().get(5);
        stepThreeTwoFour = routine.descendants().get(6);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        assertRoutineStep(
                stepThreeTwoOne,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.EXCLUDED);

        assertRoutineStep(
                stepThreeTwoTwo,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.SKIPPED);

        assertRoutineStep(
                stepThreeTwoThree,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.NOT_STARTED);

        assertRoutineStep(
                stepThreeTwoFour,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.NOT_STARTED);

        // Skip ThreeTwo
        LocalDateTime time8 = LocalDateTime.now();
        routine = doRoutine(stepThreeTwo.id(),
                time8,
                routineService::skipStep);
        stepThreeTwo = routine.descendants().get(2);
        stepThreeTwoOne = routine.descendants().get(3);
        stepThreeTwoTwo = routine.descendants().get(4);
        stepThreeTwoThree = routine.descendants().get(5);
        stepThreeTwoFour = routine.descendants().get(6);
        RoutineStep stepThreeThree = routine.descendants().get(7);

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time0);

        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.SKIPPED);

        assertRoutineStep(
                stepThreeTwoOne,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.EXCLUDED);

        assertRoutineStep(
                stepThreeTwoTwo,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.SKIPPED);

        assertRoutineStep(
                stepThreeTwoThree,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.NOT_STARTED);

        assertRoutineStep(
                stepThreeTwoFour,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.NOT_STARTED);

        assertRoutineStep(
                stepThreeThree,
                THREETHREE,
                TimeableStatus.COMPLETED);

        // Jump to ThreeTwo
        position = routine.currentPosition();
        LocalDateTime time9 = LocalDateTime.now();
        routine = doRoutine(stepThreeTwo.id(),
                time9,
                routineService::jumpToStepAndStartIfReady);
        RoutineStep stepThree = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        assertRoutineStep(
                stepThree,
                THREE_AND_FIVE,
                TimeableStatus.DESCENDANT_ACTIVE);

        // Complete ThreeTwo
        position = routine.currentPosition();
        LocalDateTime time10 = LocalDateTime.now();
        routine = doRoutine(stepThreeTwo.id(),
                time10,
                routineService::completeStep);
        stepThreeTwo = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                4,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time6);

        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.DESCENDANT_ACTIVE);

        position = routine.currentPosition();
        LocalDateTime time11 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time11,
                routineService::completeStep);
        stepThreeTwoTwo = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                5,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time11);

        assertRoutineStep(
                stepThreeTwoTwo,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED);

        position = routine.currentPosition();
        LocalDateTime time12 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time12,
                routineService::completeStep);
        stepThreeTwoThree = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                6,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time12);

        assertRoutineStep(
                stepThreeTwoThree,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.COMPLETED);

        position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::postponeStep);
        stepThreeTwoFour = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                2,
                THREETWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time1);

        assertRoutineStep(
                stepThreeTwoFour,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.POSTPONED);

        position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::excludeStep);
        stepThreeTwo = routine.descendants().get(position);

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time0);

        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.EXCLUDED);

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                0,
                THREE_AND_FIVE,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED,
                time0);
    }

    private Routine startRoutineTwoTwo() {
        TaskID rootId = tasks.get(TWOTWO).id();
        Routine routine = taskRoutineCreationTest(TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

        RoutineStep rootStep = routine.children().get(0);
        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);

        RoutineStep twoTwoTwo = routine.currentStep().children().get(1);

        assertEquals(twoTwoTwo.task().id(), tasks.get(TWOTWOTWO).id());

        return routine;
    }

    @Test
    void executeRoutineParentAllChildrenCompleted() {
        Routine routine = startRoutineTwoTwo();

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
                TimeableStatus.DESCENDANT_ACTIVE);

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

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOTWO,
                TimeableStatus.COMPLETED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED);
    }

    @Test
    void executeRoutineParentNoChildren() {
        TaskID rootId = tasks.get(TWOTWOONE).id();
        Routine routine = taskRoutineCreationTest(TWOTWOONE,
                List.of(
                        TWOTWOONE));

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWOONE,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);

        StepID previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        RoutineStep previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWOONE,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED);

        assertRoutineStep(
                previous,
                TWOTWOONE,
                TimeableStatus.COMPLETED);
    }

    @Test
    void executeRoutineParentAtLeastOneTaskSkipped() {
        Routine routine = startRoutineTwoTwo();

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
                TimeableStatus.DESCENDANT_ACTIVE);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::skipStep);
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
                TimeableStatus.SKIPPED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOTWO,
                TimeableStatus.COMPLETED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED);

        previousId = routine.currentStep().id();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);
        previous = routine.steps().get(previousId);

        assertRoutineStepExecution(
                routine,
                1,
                TWOTWOONE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        assertRoutineStep(
                previous,
                TWOTWO,
                TimeableStatus.DESCENDANT_ACTIVE);
    }

    public Routine iterateCompleteStep(Routine routine, int expectedNextPosition,
                                    String expectedNextName, TimeableStatus expectedNextStatus,
                                    String expectedPreviousName, TimeableStatus expectedPreviousStatus) {
        int position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::completeStep);

        assertRoutineStepExecution(
                routine,
                expectedNextPosition,
                expectedNextName,
                TimeableStatus.ACTIVE,
                expectedNextStatus);

        RoutineStep previousStep = routine.descendants().get(position);
        assertRoutineStep(
                previousStep,
                expectedPreviousName,
                expectedPreviousStatus);

        return routine;
    }

    @Test
    void testExecuteRoutineNestedActiveStep() {
        TaskID rootId = tasks.get(TWO).id();
        routineService.refreshRoutines(false);

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);
        assertNull(routine.currentStep().parentId());

        routine = doRoutine(routine.descendants().get(6).id(),
                LocalDateTime.now(),
                routineService::excludeStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);
        assertNull(routine.currentStep().parentId());

        RoutineStep previousStep = routine.descendants().get(6);
        assertRoutineStep(
                previousStep,
                TWOTHREE,
                TimeableStatus.EXCLUDED);

        routine = iterateCompleteStep(routine, 1,
                TWOONE, TimeableStatus.ACTIVE,
                TWO, TimeableStatus.DESCENDANT_ACTIVE);

        routine = iterateCompleteStep(routine, 2,
                TWOTWO, TimeableStatus.ACTIVE,
                TWOONE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 3,
                TWOTWOONE, TimeableStatus.ACTIVE,
                TWOTWO, TimeableStatus.DESCENDANT_ACTIVE);

        routine = iterateCompleteStep(routine, 4,
                TWOTWOTWO, TimeableStatus.ACTIVE,
                TWOTWOONE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 2,
                TWOTWO, TimeableStatus.ACTIVE,
                TWOTWOTWO, TimeableStatus.COMPLETED);

        assertEquals(routine.descendants().get(0).status(), TimeableStatus.DESCENDANT_ACTIVE);

        iterateCompleteStep(routine, 0,
                TWO, TimeableStatus.ACTIVE,
                TWOTWO, TimeableStatus.COMPLETED);
    }

    @Test
    void testExecuteRoutineSkipThenExcludeFirstStep() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);

        assertNull(routine.currentStep().parentId());

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE);

        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::excludeStep);

        assertRoutineStepExecution(
                routine,
                0,
                TWOTWO,
                TimeableStatus.EXCLUDED,
                TimeableStatus.EXCLUDED);
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersistAtBack() {
        Routine routine = taskRoutineCreationTest(TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        Response response = changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id()))));
        assertTrue(response.success());

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
    @Transactional
    void testRoutineRecalculateWithPersistAtFront() {
        Routine routine = taskRoutineCreationTest(TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id())
                        .position(0))));

        routine = routineService.fetchRoutine(routine.id());

        assertFreshRoutine(List.of(
                        TWOTWO,
                        THREEONE,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithDelete() {
        Routine routine = taskRoutineCreationTest(TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

        Duration originalDuration = routine.estimatedDuration();

        changeService.execute(Request.of(new DeleteChange<>(
                ID.of(links.get(Triple.of(TWOTWO, TWOTWOONE, 0))))));

//        assertEquals(2, queryService.fetchChildCount(root.id()));

        routine = routineService.fetchRoutine(routine.id());

        assertFreshRoutine(List.of(
                        TWOTWO,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(originalDuration.minus(Duration.ofMinutes(30)), routine.estimatedDuration());
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithMerge() {
        Routine routine = taskRoutineCreationTest(TWOTWO,
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

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
    void testCreateRoutineFromProjectLinkWithLimitingStepCountViaFilter() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        Change resetDurationLimitChange = new MergeChange<>(
                node.projectDurationLimit(Optional.empty()));

        node = (TaskNode) changeService.execute(Request.of(resetDurationLimitChange))
                .changeRelevantDataMap().get(resetDurationLimitChange.id()).get(0);
        assertTrue(node.projectDurationLimit().isEmpty());

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .stepCountLimit(3);
        assertTrue(filter.isLimiting());

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(TWOTWOTHREE_AND_THREETWOTWO));

        filter = new RoutineLimitFilter()
                .stepCountLimit(1);
        assertTrue(filter.isLimiting());

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
                .stepCountLimit(2);
        assertTrue(filter.isLimiting());

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO),
                List.of(
                        TWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO));

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
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingStepCountViaLink() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        Change resetDurationLimitChange = new MergeChange<>(
                node.projectDurationLimit(Optional.empty())
                        .projectStepCountLimit(Optional.of(3)));

        node = (TaskNode) changeService.execute(Request.of(resetDurationLimitChange))
                .changeRelevantDataMap().get(resetDurationLimitChange.id()).get(0);
        assertTrue(node.projectDurationLimit().isEmpty());

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
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
                node.projectStepCountLimit(Optional.of(1)))));

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
                node.projectStepCountLimit(Optional.of(2)))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO),
                List.of(
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE));

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
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitingStepCount() {
        TaskNode two = nodes.get(Triple.of(NULL, TWO, 1));
        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));

        Change resetDurationLimitChange = new MergeChange<>(
                twoTwo.projectDurationLimit(Optional.empty())
                        .projectStepCountLimit(Optional.of(3)));

        DataMapResponse response = changeService.execute(Request.of(resetDurationLimitChange,
                        new MergeChange<>(
                                two.projectDurationLimit(Optional.empty()))));

        twoTwo = (TaskNode) response.changeRelevantDataMap().get(resetDurationLimitChange.id()).get(0);
        assertTrue(twoTwo.projectDurationLimit().isEmpty());

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
                id -> Duration.ofHours(5).plus(
                        Duration.ofMinutes(30)),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTHREE),
                List.of(TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo.projectStepCountLimit(Optional.of(2)))));

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
    }

    @Test
    void testSkipStepWithParentWithoutMarkingDescendantsAsSkipped() {
        TaskID rootId = tasks.get(TWO).id();
        routineService.refreshRoutines(false);

        Routine routine = routineService.createRoutine(rootId, clock.time()).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertRoutineStepExecution(
                routine,
                0,
                TWO,
                TimeableStatus.NOT_STARTED,
                TimeableStatus.NOT_STARTED,
                null);
        assertNull(routine.currentStep().parentId());

        assertRoutineStep(routine.descendants().get(5),
                TWOTWOTHREE_AND_THREETWOTWO, TimeableStatus.LIMIT_EXCEEDED);

        routine = iterateCompleteStep(routine, 1,
                TWOONE, TimeableStatus.ACTIVE,
                TWO, TimeableStatus.DESCENDANT_ACTIVE);

        routine = iterateCompleteStep(routine, 2,
                TWOTWO, TimeableStatus.ACTIVE,
                TWOONE, TimeableStatus.COMPLETED);

        routine = iterateStep(routine, 6,
                TWOTHREE, TimeableStatus.ACTIVE,
                TWOTWO, TimeableStatus.SKIPPED,
                routineService::skipStep);

        assertRoutineStep(routine.descendants().get(3),
                TWOTWOONE, TimeableStatus.NOT_STARTED);

        assertRoutineStep(routine.descendants().get(4),
                TWOTWOTWO, TimeableStatus.NOT_STARTED);

        assertRoutineStep(routine.descendants().get(5),
                TWOTWOTHREE_AND_THREETWOTWO, TimeableStatus.LIMIT_EXCEEDED);
    }
}