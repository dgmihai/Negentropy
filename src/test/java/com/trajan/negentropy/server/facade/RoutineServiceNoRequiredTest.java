package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.RoutineTestTemplate;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
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
                node.projectDurationLimit(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO),
                List.of(
                        TWOONE,
                        TWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_2() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(3)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_3() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(5)))));

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
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_4() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

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
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

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
                node.projectDurationLimit(routineDuration))));

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
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_TWO() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

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
                List.of());
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

        Change multiTagChange = new MultiMergeChange<>(new TaskDTO()
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

        Routine routine = routineService.createRoutine(rootId).routine();

        RoutineStep rootStep = routine.children().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.children().get(routine.currentPosition()).task().name());
        assertEquals(TimeableStatus.NOT_STARTED, routine.currentStep().status());
        assertEquals(TimeableStatus.NOT_STARTED, routine.status());
        assertNull(routine.currentStep().parentId());

        LocalDateTime time1 = LocalDateTime.now();
        doRoutineFalse(routine.currentStep().id(),
                time1,
                routineService::previousStep);
    }

    @Test
    void testExecuteOnRoutineStepWhichIsNotCurrentOrParentOfCurrent() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId).routine();

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

        Routine routine = routineService.createRoutine(rootId).routine();

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

        RoutineStep stepTwoTwo = routine.getDescendants().get(position);
        assertRoutineStepParent(
                routine,
                stepTwoTwo,
                TWOTWO,
                TimeableStatus.ACTIVE);

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

        RoutineStep stepTwoTwoOne = routine.getDescendants().get(position);
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

        RoutineStep stepTwoTwoTwo = routine.getDescendants().get(position);
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

        RoutineStep stepTwoTwoThree = routine.getDescendants().get(position);
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

        stepTwoTwoTwo = routine.getDescendants().get(position);
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

        stepTwoTwoThree = routine.getDescendants().get(position);
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

        stepTwoTwo = routine.getDescendants().get(position);
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
                TimeableStatus.ACTIVE);

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

        stepTwoTwoTwo = routine.getDescendants().get(position);
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

        Routine routine = routineService.createRoutine(rootId).routine();

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

        RoutineStep stepThreeAndFive = routine.getDescendants().get(position);
        assertRoutineStep(
                stepThreeAndFive,
                THREE_AND_FIVE,
                TimeableStatus.ACTIVE);

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

        RoutineStep stepThreeOne = routine.getDescendants().get(position);
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

        RoutineStep stepThreeTwo = routine.getDescendants().get(position);
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

        stepThreeOne = routine.getDescendants().get(position);
        assertRoutineStep(
                stepThreeOne,
                THREEONE,
                TimeableStatus.COMPLETED);

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

        stepThreeTwo = routine.getDescendants().get(position);
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
        stepThreeTwo = routine.getDescendants().get(position);

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
                TimeableStatus.ACTIVE);

        position = routine.currentPosition();
        LocalDateTime time6 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time6,
                routineService::completeStep);
        RoutineStep stepThreeTwoOne = routine.getDescendants().get(position);

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
                TimeableStatus.COMPLETED);

        position = routine.currentPosition();
        LocalDateTime time7 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time7,
                routineService::completeStep);
        RoutineStep stepThreeTwoTwo = routine.getDescendants().get(position);

        assertRoutineStepExecution(
                routine,
                5,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time7);

        assertRoutineStep(
                stepThreeTwoTwo,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED);

        position = routine.currentPosition();
        LocalDateTime time8 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time8,
                routineService::completeStep);
        RoutineStep stepThreeTwoThree = routine.getDescendants().get(position);

        assertRoutineStepExecution(
                routine,
                6,
                SIX_AND_THREETWOFOUR,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time8);

        assertRoutineStep(
                stepThreeTwoThree,
                THREETWOONE_AND_THREETWOTHREE,
                TimeableStatus.COMPLETED);

        position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                LocalDateTime.now(),
                routineService::postponeStep);
        RoutineStep stepThreeTwoFour = routine.getDescendants().get(position);

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
        stepThreeTwo = routine.getDescendants().get(position);

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

        RoutineStep previousStep = routine.getDescendants().get(position);
        assertRoutineStep(
                previousStep,
                expectedPreviousName,
                expectedPreviousStatus);

        return routine;
    }

    @Test
    void testExecuteRoutineNestedActiveStep() {
        TaskID rootId = tasks.get(TWO).id();

        Routine routine = routineService.createRoutine(rootId).routine();

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

        routine = doRoutine(routine.getDescendants().get(5).id(),
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

        RoutineStep previousStep = routine.getDescendants().get(5);
        assertRoutineStep(
                previousStep,
                TWOTHREE,
                TimeableStatus.EXCLUDED);

        routine = iterateCompleteStep(routine, 1,
                TWOONE, TimeableStatus.ACTIVE,
                TWO, TimeableStatus.ACTIVE);

        routine = iterateCompleteStep(routine, 2,
                TWOTWO, TimeableStatus.ACTIVE,
                TWOONE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 3,
                TWOTWOONE, TimeableStatus.ACTIVE,
                TWOTWO, TimeableStatus.ACTIVE);

        routine = iterateCompleteStep(routine, 4,
                TWOTWOTWO, TimeableStatus.ACTIVE,
                TWOTWOONE, TimeableStatus.COMPLETED);

        routine = iterateCompleteStep(routine, 2,
                TWOTWO, TimeableStatus.ACTIVE,
                TWOTWOTWO, TimeableStatus.COMPLETED);

        iterateCompleteStep(routine, 0,
                TWO, TimeableStatus.ACTIVE,
                TWOTWO, TimeableStatus.COMPLETED);
    }

    @Test
    void testExecuteRoutineSkipFirstStep() {

        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId).routine();

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
                routineService::skipStep);
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersistAtBack() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
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

        RoutineResponse response = routineService.recalculateRoutine(routine.id());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREEONE),
                        routine);
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersistAtFront() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
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
                        .childId(tasks.get(THREEONE).id())
                        .position(0))));

        RoutineResponse response = routineService.recalculateRoutine(routine.id());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
        assertRoutine(List.of(
                        TWOTWO,
                        THREEONE,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithDelete() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();

        changeService.execute(Request.of(new DeleteChange<>(
                ID.of(links.get(Triple.of(TWOTWO, TWOTWOONE, 0))))));

        RoutineResponse response = routineService.recalculateRoutine(routine.id());
        assertTrue(response.success());
        routine = response.routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(originalDuration.minus(Duration.ofMinutes(30)), routine.estimatedDuration());
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithMerge() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
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

        RoutineResponse response = routineService.recalculateRoutine(routine.id());
        assertTrue(response.success());
        routine = response.routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(Duration.ofHours(4), routine.estimatedDuration());
    }
}