package com.trajan.negentropy.server.facade;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.RoutineTestTemplateNoRequiredTasks;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.RoutineUtil;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoutineServiceNoRequiredTest extends RoutineTestTemplateNoRequiredTasks {

    @Autowired private RoutineService routineService;

    @BeforeAll
    void setup() {
        init();
    }

    private void assertRoutine(List<String> expectedSteps, RoutineResponse response) {
        assertTrue(response.success());

        assertRoutine(expectedSteps, response.routine());
    }

    private void assertRoutine(List<String> expectedSteps, Routine routine) {
        List<String> actual = routine.getAllChildren().stream()
                .map(RoutineStep::name)
                .toList();

        assertEquals(expectedSteps, actual);
        assertTrue(Iterables.elementsEqual(expectedSteps, actual));

        for (RoutineStep step : routine.children()) {
            assertEquals(TimeableStatus.NOT_STARTED, step.status());
        }
    }


    private void linkRoutineCreationTestWithExpectedDuration(Triple<String, String, Integer> rootLink,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps) {
                TaskNode node = nodes.get(rootLink);
                RoutineResponse response = routineService.createRoutine(node.linkId());
                assertRoutine(expectedSteps, response);
                assertEquals(
                        expectedDuration.apply(node.child().id()),
                        response.routine().estimatedDuration());
    }

    private void taskRoutineCreationTestWithExpectedDuration(String rootTask,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps) {
        TaskID rootId = tasks.get(rootTask).id();
        RoutineResponse response = routineService.createRoutine(rootId);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(rootId),
                response.routine().estimatedDuration());
    }

    private void taskRoutineCreationTestWithExpectedDurationAndFilter(String rootTask,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             TaskNodeTreeFilter filter,
                                                             List<String> expectedSteps) {
        TaskID rootId = tasks.get(rootTask).id();
        RoutineResponse response = routineService.createRoutine(rootId, filter);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(rootId),
                response.routine().estimatedDuration());
    }

    private void linkRoutineCreationTestWithExpectedDurationAndFilter(Triple<String, String, Integer> rootLink,
                                                                      Function<TaskID, Duration> expectedDuration,
                                                                      TaskNodeTreeFilter filter,
                                                                      List<String> expectedSteps) {
        TaskNode node = nodes.get(rootLink);
        RoutineResponse response = routineService.createRoutine(node.linkId(), filter);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(node.child().id()),
                response.routine().estimatedDuration());
    }

    private void taskRoutineCreationTest (String rootTask, List<String> expectedSteps) {
        taskRoutineCreationTestWithExpectedDuration(
                rootTask,
                rootId -> entityQueryService.getNetDuration(rootId).val(),
                expectedSteps);
    }

    private void linkRoutineCreationTest(Triple<String, String, Integer> rootLink, List<String> expectedSteps) {
        linkRoutineCreationTestWithExpectedDuration(
                rootLink,
                rootTaskId -> entityQueryService.getNetDuration(rootTaskId).val(),
                expectedSteps);
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
    void testCreateRoutineFromLink() {
        linkRoutineCreationTest(
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                List.of(
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR));
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
    void testCreateRoutineFromProjectLinkWithNestedLimitedProject() {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                id -> Duration.ofHours(2),
                List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_1() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(2);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_2() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(Duration.ofHours(3)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_3() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(Duration.ofHours(5)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_4() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(routineDuration))));

        TaskNodeTreeFilter filter = new TaskNodeTreeFilter();
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
                node.projectDuration(routineDuration))));

        TaskNodeTreeFilter filter = new TaskNodeTreeFilter();
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
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDuration(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE));
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
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter();
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
                        THREETHREE));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualNonLimitingProjectDuration_THREE_AND_FIVE() {
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter();
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
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() {
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
                        SIX_AND_THREETWOFOUR));
    }

    @Test
    void testCreateRoutineFromDifferentProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 4),
                id -> Duration.ofMinutes(210),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE));
    }

    private Routine doRoutine(StepID stepId, LocalDateTime time,
                              BiFunction<StepID, LocalDateTime, RoutineResponse> routineCall) {
        RoutineResponse response = routineCall.apply(stepId, time);
        assertTrue(response.success());
        return response.routine();
    }

    private void doRoutineFalse(StepID stepId, LocalDateTime time,
                              BiFunction<StepID, LocalDateTime, RoutineResponse> routineCall) {
        RoutineResponse response = routineCall.apply(stepId, time);
        assertFalse(response.success());
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

    private void assertRoutineStepExecution(Routine routine, int expectedPosition, String expectedStepName,
                                            TimeableStatus expectedStatus, TimeableStatus expectedRoutineStatus,
                                            LocalDateTime time) {
        assertEquals(expectedPosition, routine.currentPosition());
        assertRoutineStep(routine.currentStep(), expectedStepName, expectedStatus);
        assertEquals(expectedRoutineStatus, routine.status());
        assertEquals(time, routine.currentStep().startTime());
    }

    private void assertRoutineStepParent(Routine routine, RoutineStep parent, String expectedStepName,
                                         TimeableStatus expectedStatus) {
        assertRoutineStep(parent, expectedStepName, expectedStatus);
        assertEquals(parent.id(), routine.currentStep().parentId());
    }

    private void assertRoutineStep(RoutineStep step, String expectedStepName, TimeableStatus expectedStatus) {
        assertEquals(expectedStepName, step.task().name());
        assertEquals(expectedStatus, step.status());
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

        RoutineStep stepTwoTwo = routine.getAllChildren().get(position);
        assertRoutineStepParent(
                routine,
                stepTwoTwo,
                TWOTWO,
                TimeableStatus.ACTIVE);

        assertEquals(
                Duration.between(time1, time2),
                RoutineUtil.getStepElapsedActiveDuration(stepTwoTwo, time2));

        LocalDateTime now = LocalDateTime.now();
        assertEquals(
                Duration.between(time2, now),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), now));

        position = routine.currentPosition();
        LocalDateTime time3 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time3,
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                2,
                TWOTWOTWO,
                TimeableStatus.ACTIVE,
                TimeableStatus.ACTIVE,
                time3);

        RoutineStep stepTwoTwoOne = routine.getAllChildren().get(position);
        assertRoutineStep(
                stepTwoTwoOne,
                TWOTWOONE,
                TimeableStatus.SKIPPED);

        assertEquals(0, stepTwoTwoOne.children().size());
        assertEquals(time3, stepTwoTwoOne.finishTime());
        assertEquals(time3, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time2, time3),
                RoutineUtil.getStepElapsedActiveDuration(stepTwoTwoOne, time3));

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
                TimeableStatus.SKIPPED);

        assertEquals(time4, routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time3, time4),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time4));

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
        assertEquals(time5, routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time4, time5),
                routine.currentStep().elapsedSuspendedDuration());
        assertEquals(
                Duration.between(time3, time5).minus(routine.currentStep().elapsedSuspendedDuration()),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time5));

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

        RoutineStep stepTwoTwoTwo = routine.getAllChildren().get(position);
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

        RoutineStep stepTwoTwoThree = routine.getAllChildren().get(position);
        assertRoutineStep(
                stepTwoTwoThree,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.SUSPENDED);

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

        stepTwoTwoTwo = routine.getAllChildren().get(position);
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
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED,
                time6);

        assertEquals(position, routine.currentPosition());
        assertEquals(TimeableStatus.SKIPPED, routine.getAllChildren().get(routine.currentPosition() - 1).status());
        assertEquals(time9, routine.getAllChildren().get(routine.currentPosition()).finishTime());

        position = routine.currentPosition();
        LocalDateTime time10 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time10,
                routineService::skipStep);

        assertRoutineStepExecution(
                routine,
                3,
                TWOTWOTHREE_AND_THREETWOTWO,
                TimeableStatus.COMPLETED,
                TimeableStatus.COMPLETED,
                time6);

        assertEquals(position, routine.currentPosition());
        assertEquals(TimeableStatus.SKIPPED, routine.getAllChildren().get(routine.currentPosition() - 1).status());
        assertEquals(time10, routine.getAllChildren().get(routine.currentPosition()).finishTime());
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
        // TODO: Test skipping first step

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

        RoutineStep stepThreeAndFive = routine.getAllChildren().get(position);
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

        RoutineStep stepThreeOne = routine.getAllChildren().get(position);
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

        RoutineStep stepThreeTwo = routine.getAllChildren().get(position);
        assertRoutineStep(
                stepThreeTwo,
                THREETWO,
                TimeableStatus.SUSPENDED);

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

        stepThreeOne = routine.getAllChildren().get(position);
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

        stepThreeTwo = routine.getAllChildren().get(position);
        for (RoutineStep step : stepThreeTwo.children()) {
            assertEquals(TimeableStatus.SKIPPED, step.status());
        }
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersist() {
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

        RoutineResponse response = routineService.recalculateRoutine(routine.id(), LocalDateTime.now());
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

        RoutineResponse response = routineService.recalculateRoutine(routine.id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(routine.estimatedDuration(), originalDuration.minus(Duration.ofMinutes(30)));

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
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

        Task twotwoone = tasks.get(TWOTWOONE);
        changeService.execute(Request.of(new MergeChange<>(
                new Task(twotwoone.id())
                        .duration(twotwoone.duration().plus(Duration.ofHours(1))))));

        RoutineResponse response = routineService.recalculateRoutine(routine.id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofHours(1)));

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
    }
}