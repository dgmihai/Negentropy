package com.trajan.negentropy.server;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.SerializationUtil;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.TimeableUtil;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RoutineTestTemplate extends TaskTestTemplate {
    @Autowired protected RoutineService routineService;
    @Autowired protected TimeableUtil timeableUtil;

    @Override
    protected void initTasks(String parent, List<Pair<Task, TaskNodeDTO>> children) {
        children.forEach(pair -> pair.getFirst().required(false));
        super.initTasks(parent, children);
    }

    @Override
    protected void init() {
        initTasks(
                null,
                List.of(Pair.of(
                                new Task()
                                        .name(ONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWO)
                                        .duration(Duration.ofHours(2))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDurationLimit(Optional.of(Duration.ofHours(2)))
                        ), Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .duration(Duration.ofHours(3))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDurationLimit(Optional.of(Duration.ofMinutes(13*60+30)))
                        ), Pair.of(
                                new Task()
                                        .name(FOUR)
                                        .duration(Duration.ofHours(4)),
                                new TaskNodeDTO()
                        ),Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDurationLimit(Optional.of(Duration.ofHours(5)))
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR)
                                        .duration(Duration.ofHours(6)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                TWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ),Pair.of(
                                new Task()
                                        .name(TWOTWO)
                                        .duration(Duration.ofHours(1))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDurationLimit(Optional.of(Duration.ofHours(2)))
                        ), Pair.of(
                                new Task()
                                        .name(TWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                TWOTWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOTWOONE)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTWO)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                THREE_AND_FIVE,
                List.of(Pair.of(
                                new Task()
                                        .name(THREEONE)
                                        .duration(Duration.ofMinutes(30))
                                        .project(true),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWO)
                                        .duration(Duration.ofHours(1))
                                        .project(true),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETHREE)
                                        .duration(Duration.ofHours(1))
                                        .project(true),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                THREETWO,
                List.of(Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR),
                                new TaskNodeDTO()
                        )
                )
        );

        refreshMaps();
    }

    protected void assertRoutineAll(List<String> expectedSteps, Routine routine) {
        List<String> actual = routine.descendants().stream()
                .map(RoutineStep::name)
                .toList();

        assertEquals(expectedSteps, actual);
        assertTrue(Iterables.elementsEqual(expectedSteps, actual));
    }

    protected void assertRoutine(List<String> expectedSteps, Routine routine) {
        List<String> actual = routine.descendants().stream()
                .filter(step -> step.status() != TimeableStatus.LIMIT_EXCEEDED)
                .map(RoutineStep::name)
                .toList();

        assertEquals(expectedSteps, actual);
        assertTrue(Iterables.elementsEqual(expectedSteps, actual));
    }

    protected void assertRoutineWithExceeded(List<String> expectedSteps, List<String> expectedStepsWithStatus,
                                           Routine routine, TaskNodeTreeFilter filter) {
        assertRoutineWithStatus(expectedSteps, TimeableStatus.LIMIT_EXCEEDED, expectedStepsWithStatus, routine, filter);
    }

    protected void assertRoutineWithStatus(List<String> expectedSteps, TimeableStatus status, List<String> expectedStepsWithStatus,
                                           Routine routine, TaskNodeTreeFilter filter) {
        assertRoutine(expectedSteps, routine);
        assertLinksByStatus(status, expectedStepsWithStatus, routine, filter);
    }

    protected void assertRefreshRoutineWithStatus(TimeableStatus status, List<String> expectedSteps, List<String> expectedExceededSteps, Routine routine) {
        assertRoutine(expectedSteps, routine);
        assertLinksFromRoutineByStatus(status, Set.copyOf(expectedExceededSteps), routine);
    }

    protected void assertFreshRoutine(List<String> expectedSteps, RoutineResponse response) {
        assertTrue(response.success());

        assertFreshRoutine(expectedSteps, response.routine());
    }

    protected void assertFreshRoutine(List<String> expectedSteps, Routine routine) {
        assertRoutine(expectedSteps, routine);

        for (RoutineStep step : routine.children()) {
            if (expectedSteps.contains(step.name())) {
                assertEquals(TimeableStatus.NOT_STARTED, step.status());
            } else {
                assertEquals(TimeableStatus.LIMIT_EXCEEDED, step.status());
            }
        }
    }

    protected Routine linkRoutineCreationTestWithExpectedDuration(Triple<String, String, Integer> rootLink,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps,
                                                             List<String> exceededTasks) throws Exception {
        return linkRoutineCreationTestWithExpectedDuration(rootLink, null, expectedDuration, expectedSteps, exceededTasks);
    }

    protected Routine linkRoutineCreationTestWithExpectedDuration(Triple<String, String, Integer> rootLink,
                                                             TaskNodeTreeFilter filter,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps,
                                                             List<String> exceededTasks) throws Exception {
        TaskNode node = nodes.get(rootLink);
        RoutineResponse response = routineService.createRoutine(node.linkId(), filter, clock.time());
        assertTrue(response.success());
        assertFreshRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(node.child().id()),
                timeableUtil.getRemainingNestedDuration(response.routine().children().get(0), response.routine().startTime()));

        RoutineEntity routineEntity = entityQueryService.getRoutine(response.routine().id());
        RoutineLimitFilter resultFilter = (RoutineLimitFilter) SerializationUtil.deserialize(routineEntity.serializedFilter());
        if (filter != null) {
            assertEquals(resultFilter, RoutineLimitFilter.parse(filter));
        }

        assertLinksByStatus(TimeableStatus.LIMIT_EXCEEDED, exceededTasks, routineEntity, filter);

        return response.routine();
    }

    protected void assertLinksByStatus(TimeableStatus status, List<String> expectedTasks, Routine routine, TaskNodeTreeFilter filter) {
        Set<String> expectedTaskSet = Set.copyOf(expectedTasks);
        assertLinksFromRoutineByStatus(status, expectedTaskSet, routine);
    }

    protected void assertLinksByStatus(TimeableStatus status, List<String> expectedTasks, RoutineEntity routineEntity, TaskNodeTreeFilter filter) {
        Set<String> expectedTaskSet = Set.copyOf(expectedTasks);
        assertLinksFromRoutineByStatus(status, expectedTaskSet, routineEntity);
    }

    protected void assertLinksFromRoutineByStatus(TimeableStatus status, Set<String> expectedTasks, Routine routine) {
        Set<String> actualStepsByStatus = routine.steps().values().stream()
                .filter(step -> step.task().project())
                .flatMap(step -> step.children().stream())
                .filter(step -> step.status().equals(status))
                .map(RoutineStep::name)
                .collect(Collectors.toSet());

        assertEquals(expectedTasks, actualStepsByStatus);
    }

    protected void assertLinksFromRoutineByStatus(TimeableStatus status, Set<String> expectedTasks, RoutineEntity routine) {
        Set<String> actualStepsByStatus = routine.descendants().stream()
                .filter(step -> step.task().project())
                .flatMap(step -> step.children().stream())
                .filter(step -> step.status().equals(status))
                .map(RoutineStepEntity::name)
                .collect(Collectors.toSet());

        assertEquals(expectedTasks, actualStepsByStatus);
    }

    protected void assertLinksFromRoutineByStatus(TimeableStatus status, List<String> expectedTasks, List<LinkID> actualNodeIds) {
        List<Task> actualExcludedTasks = queryService.fetchNodes(actualNodeIds)
                .map(TaskNode::task)
                .toList();

        System.out.println("EXPECT WITH " + status.name() + ": " + expectedTasks);
        System.out.println("ACTUAL WITH " + status.name() + ": " + actualExcludedTasks
                .stream()
                .map(Task::name)
                .toList());

        assertEquals(expectedTasks.size(), actualExcludedTasks.size());
        for (Task task : actualExcludedTasks) {
            assertTrue(expectedTasks.contains(task.name()));
        }
    }

    protected Routine taskRoutineCreationTestWithExpectedDuration(String rootTask,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps) {
        TaskID rootId = tasks.get(rootTask).id();
        RoutineResponse response = routineService.createRoutine(rootId, clock.time());
        assertTrue(response.success());
        assertFreshRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(rootId),
                response.routine().estimatedDuration());
        return response.routine();
    }

    protected Routine taskRoutineCreationTestWithExpectedDurationAndFilter(String rootTask,
                                                                      Function<TaskID, Duration> expectedDuration,
                                                                      TaskNodeTreeFilter filter,
                                                                      List<String> expectedSteps) {
        TaskID rootId = tasks.get(rootTask).id();
        RoutineResponse response = routineService.createRoutine(rootId, filter, clock.time());
        assertTrue(response.success());
        assertFreshRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(rootId),
                response.routine().estimatedDuration());
        return response.routine();
    }

    protected Routine linkRoutineCreationTestWithExpectedDurationAndFilter(Triple<String, String, Integer> rootLink,
                                                                      Function<TaskID, Duration> expectedDuration,
                                                                      TaskNodeTreeFilter filter,
                                                                      List<String> expectedSteps,
                                                                      List<String> exceededTasks) {
        TaskNode node = nodes.get(rootLink);
        RoutineResponse response = routineService.createRoutine(node.linkId(), filter, clock.time());
        assertTrue(response.success());
        assertFreshRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(node.child().id()),
                response.routine().estimatedDuration());

        assertLinksByStatus(TimeableStatus.LIMIT_EXCEEDED, exceededTasks, response.routine(), filter);

        return response.routine();
    }

    protected Routine taskRoutineCreationTest (String rootTask, List<String> expectedSteps) {
        return taskRoutineCreationTestWithExpectedDuration(
                rootTask,
                rootId -> queryService.fetchNetDuration(rootId, null),
                expectedSteps);
    }

    protected Routine linkRoutineCreationTest(Triple<String, String, Integer> rootLink, List<String> expectedSteps, List<String> excludedTasks)
            throws Exception {
        return linkRoutineCreationTestWithExpectedDuration(
                rootLink,
                rootTaskId -> queryService.fetchNetDuration(rootTaskId, null),
                expectedSteps,
                excludedTasks);
    }

    protected Routine doRoutine(StepID stepId, LocalDateTime time,
                                BiFunction<StepID, LocalDateTime, RoutineResponse> routineCall) {
        RoutineResponse response = routineCall.apply(stepId, time);
        assertTrue(response.success());
        return response.routine();
    }

    protected void doRoutineFalse(StepID stepId, LocalDateTime time,
                                BiFunction<StepID, LocalDateTime, RoutineResponse> routineCall) {
        RoutineResponse response = routineCall.apply(stepId, time);
        assertFalse(response.success());
    }


    protected void assertRoutineStepExecution(Routine routine, int expectedPosition, String expectedStepName,
                                            TimeableStatus expectedStatus, TimeableStatus expectedRoutineStatus) {
        assertRoutineStep(routine.currentStep(), expectedStepName, expectedStatus);
        assertEquals(expectedPosition, routine.currentPosition());
        assertEquals(expectedRoutineStatus, routine.status());
    }

    protected void assertRoutineStepExecution(Routine routine, int expectedPosition, String expectedStepName,
                                            TimeableStatus expectedStatus, TimeableStatus expectedRoutineStatus,
                                            LocalDateTime time) {
        assertRoutineStepExecution(routine, expectedPosition, expectedStepName, expectedStatus, expectedRoutineStatus);
        assertEquals(time, routine.currentStep().startTime());
    }

    protected void assertRoutineStepParent(Routine routine, RoutineStep parent, String expectedStepName,
                                         TimeableStatus expectedStatus) {
        assertRoutineStep(parent, expectedStepName, expectedStatus);
        assertEquals(parent.id(), routine.currentStep().parentId());
    }

    protected void assertRoutineStep(RoutineStep step, String expectedStepName, TimeableStatus expectedStatus) {
        assertEquals(expectedStepName, step.task().name());
        assertEquals(expectedStatus, step.status());
    }


    protected Routine iterateCompleteStep(Routine routine, int expectedNextPosition,
                                       String expectedNextName, TimeableStatus expectedNextStatus,
                                       String expectedPreviousName, TimeableStatus expectedPreviousStatus) {
        int position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                clock.time(),
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

    protected Routine iterateStep(Routine routine, int expectedNextPosition,
                                  String expectedNextName, TimeableStatus expectedNextStatus,
                                  String expectedPreviousName, TimeableStatus expectedPreviousStatus,
                                  BiFunction<StepID, LocalDateTime, RoutineResponse> operation) {
        int position = routine.currentPosition();
        routine = doRoutine(routine.currentStep().id(),
                clock.time(),
                operation);

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
}
