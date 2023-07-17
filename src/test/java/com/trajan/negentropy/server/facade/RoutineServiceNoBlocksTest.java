package com.trajan.negentropy.server.facade;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.RoutineTestTemplateNoBlocks;
import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.StepID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.RoutineUtil;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoutineServiceNoBlocksTest extends RoutineTestTemplateNoBlocks {

    @Autowired private RoutineService routineService;

    @BeforeAll
    void setup() {
        init();
    }

    private void assertRoutine(List<String> expectedSteps, RoutineResponse response) {
        assertTrue(response.success());

        Routine routine = response.routine();

        assertEquals(expectedSteps, routine.steps().stream()
                .map(step -> step.task().name())
                .peek(System.out::println).toList());
        assertTrue(Iterables.elementsEqual(expectedSteps, routine.steps().stream()
                .map(step -> step.task().name())
                .peek(System.out::println).toList()));

        for (RoutineStep step : routine.steps()) {
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
                                                             TaskFilter filter,
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
                                                                      TaskFilter filter,
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
                rootId -> entityQueryService.getTotalDuration(rootId).totalDuration(),
                expectedSteps);
    }

    private void linkRoutineCreationTest(Triple<String, String, Integer> rootLink, List<String> expectedSteps) {
        linkRoutineCreationTestWithExpectedDuration(
                rootLink,
                rootTaskId -> entityQueryService.getTotalDuration(rootTaskId).totalDuration(),
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

        updateService.updateNode(node.projectDuration(routineDuration));

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

        updateService.updateNode(node.projectDuration(Duration.ofHours(3)));

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

        updateService.updateNode(node.projectDuration(Duration.ofHours(5)));

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

        updateService.updateNode(node.projectDuration(routineDuration));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        updateService.updateNode(node.projectDuration(routineDuration));

        TaskFilter filter = new TaskFilter();
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

        updateService.updateNode(node.projectDuration(routineDuration));

        TaskFilter filter = new TaskFilter();
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

        updateService.updateNode(node.projectDuration(routineDuration));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
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
        TaskFilter filter = new TaskFilter();
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
        TaskFilter filter = new TaskFilter();
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
                id -> Duration.ofMinutes(7*60 + 30),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE));
    }

    @Test
    void testCreateRoutineFromDifferentProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 4),
                id -> Duration.ofMinutes(270),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO));
    }

    private Routine doRoutine(StepID stepId, LocalDateTime time,
                              BiFunction<StepID, LocalDateTime, RoutineResponse> routineCall) {
        RoutineResponse response = routineCall.apply(stepId, time);
        assertTrue(response.success());
        return response.routine();
    }

    @Test
    void testExecuteRoutine() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId).routine();

        RoutineStep rootStep = routine.steps().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.steps().get(routine.currentPosition()).task().name());
        assertEquals(TimeableStatus.NOT_STARTED, routine.currentStep().status());
        assertEquals(TimeableStatus.NOT_STARTED, routine.status());

        LocalDateTime time1 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time1,
                routineService::startStep);

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());
        assertEquals(TimeableStatus.ACTIVE, routine.status());
        assertEquals(time1, routine.currentStep().startTime());

        LocalDateTime time2 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time2,
                routineService::completeStep);

        assertEquals(1, routine.currentPosition());
        assertEquals(TWOTWOONE, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());

        RoutineStep lastStep = routine.steps().get(routine.currentPosition() - 1);
        assertEquals(time2, lastStep.finishTime());
        assertEquals(TimeableStatus.COMPLETED, lastStep.status());
        assertEquals(time2, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time1, time2),
                RoutineUtil.getStepElapsedActiveDuration(lastStep, time2));

        LocalDateTime now = LocalDateTime.now();
        assertEquals(
                Duration.between(time2, now),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), now));

        LocalDateTime time3 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time3,
                routineService::skipStep);

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());

        lastStep = routine.steps().get(routine.currentPosition() - 1);
        assertEquals(time3, lastStep.finishTime());
        assertEquals(time3, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time2, time3),
                RoutineUtil.getStepElapsedActiveDuration(lastStep, time3));

        LocalDateTime time4 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time4,
                routineService::suspendStep);

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.SUSPENDED, routine.currentStep().status());

        lastStep = routine.steps().get(routine.currentPosition() - 1);
        assertEquals(TimeableStatus.SKIPPED, lastStep.status());
        assertEquals(time3, routine.currentStep().startTime());
        assertEquals(time4, routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time3, time4),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time4));

        LocalDateTime time5 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time5,
                routineService::startStep);

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());

        assertEquals(time3, routine.currentStep().startTime());
        assertEquals(Duration.between(time4, time5), routine.currentStep().elapsedSuspendedDuration());
        assertEquals(time5, routine.currentStep().lastSuspendedTime());

        assertEquals(
                Duration.between(time4, time5),
                routine.currentStep().elapsedSuspendedDuration());
        assertEquals(
                Duration.between(time3, time5).minus(routine.currentStep().elapsedSuspendedDuration()),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time5));

        LocalDateTime time6 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time6,
                routineService::completeStep);

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());
        assertEquals(TimeableStatus.COMPLETED, routine.steps().get(routine.currentPosition() - 1).status());
        assertEquals(time6, routine.steps().get(routine.currentPosition() - 1).finishTime());
        assertEquals(time6, routine.currentStep().startTime());

        // TODO: Failure here, occasionally
//        assertEquals(
//                Duration.between(time5, time6),
//                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time6));

        LocalDateTime time7 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time7,
                routineService::previousStep);

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());
        assertEquals(TimeableStatus.SUSPENDED, routine.steps().get(routine.currentPosition() + 1).status());
        assertNull(routine.currentStep().finishTime());
        assertEquals(time3, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time3, time7).minus(routine.currentStep().elapsedSuspendedDuration()),
                RoutineUtil.getStepElapsedActiveDuration(routine.currentStep(), time7));

        LocalDateTime time8 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time8,
                routineService::skipStep);

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.ACTIVE, routine.currentStep().status());
        assertEquals(TimeableStatus.SKIPPED, routine.steps().get(routine.currentPosition() - 1).status());
        assertEquals(time8, routine.steps().get(routine.currentPosition() - 1).finishTime());
        assertEquals(time6, routine.currentStep().startTime());

        assertEquals(
                Duration.between(time7, time8),
                routine.currentStep().elapsedSuspendedDuration());

        LocalDateTime time9 = LocalDateTime.now();
        routine = doRoutine(routine.currentStep().id(),
                time9,
                routineService::completeStep);

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(TimeableStatus.COMPLETED, routine.currentStep().status());
        assertEquals(TimeableStatus.SKIPPED, routine.steps().get(routine.currentPosition() - 1).status());
        assertEquals(time9, routine.steps().get(routine.currentPosition()).finishTime());
        assertEquals(time6, routine.currentStep().startTime());
        assertEquals(TimeableStatus.COMPLETED, routine.status());
    }
}