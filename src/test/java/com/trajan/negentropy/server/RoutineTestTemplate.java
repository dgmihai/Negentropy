package com.trajan.negentropy.server;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.SerializationUtil;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.StepID;
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
import java.util.function.BiFunction;
import java.util.function.Function;

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
                                        .projectDurationLimit(Duration.ofHours(2))
                        ), Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .duration(Duration.ofHours(3))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDurationLimit(Duration.ofMinutes(13*60+30))
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
                                        .projectDurationLimit(Duration.ofHours(5))
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
                                        .projectDurationLimit(Duration.ofHours(2))
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

//        for (TaskNode node : nodes.values()) {
//            System.out.println(node.name() + ": " + node.projectDurationLimit() + ", " + node.projectStepCountLimit() + ", " + node.projectEtaLimit());
//            assertEquals(LocalTime.MAX, node.projectEtaLimit());
//        }
    }

    protected void assertRoutine(List<String> expectedSteps, RoutineResponse response) {
        assertTrue(response.success());

        assertRoutine(expectedSteps, response.routine());
    }

    protected void assertRoutine(List<String> expectedSteps, Routine routine) {
        List<String> actual = routine.getDescendants().stream()
                .map(RoutineStep::name)
                .toList();

        assertEquals(expectedSteps, actual);
        assertTrue(Iterables.elementsEqual(expectedSteps, actual));

        for (RoutineStep step : routine.children()) {
            assertEquals(TimeableStatus.NOT_STARTED, step.status());
        }
    }

    protected void linkRoutineCreationTestWithExpectedDuration(Triple<String, String, Integer> rootLink,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps,
                                                             List<String> excludedTasks) throws Exception {
        linkRoutineCreationTestWithExpectedDuration(rootLink, null, expectedDuration, expectedSteps, excludedTasks);
    }

    protected void linkRoutineCreationTestWithExpectedDuration(Triple<String, String, Integer> rootLink,
                                                             TaskNodeTreeFilter filter,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps,
                                                             List<String> excludedTasks) throws Exception {
        TaskNode node = nodes.get(rootLink);
        RoutineResponse response = routineService.createRoutine(node.linkId(), filter);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(node.child().id()),
                timeableUtil.getRemainingNetDuration(response.routine().children().get(0), response.routine().startTime()));

        RoutineEntity routineEntity = entityQueryService.getRoutine(response.routine().id());
        RoutineLimitFilter resultFilter = (RoutineLimitFilter) SerializationUtil.deserialize(routineEntity.serializedFilter());
        if (filter != null) {
            assertEquals(resultFilter, RoutineLimitFilter.parse(filter));
        }
        List<LinkID> actualExcludedNodeIds = netDurationService.getHelper(filter).projectChildrenOutsideDurationLimitMap()
                .getOrDefault(node.id(), List.of());

        assertExcludedTasks(excludedTasks, actualExcludedNodeIds);
    }

    protected void assertExcludedTasks(List<String> expectedExcludedTasks, List<LinkID> actualExcludedNodeIds) {
        List<Task> actualExcludedTasks = queryService.fetchNodes(actualExcludedNodeIds)
                .map(TaskNode::task)
                .toList();

        System.out.println("EXPECTED EXCL'D: " + expectedExcludedTasks);
        System.out.println("ACTUAL EXCLUDED: " + actualExcludedTasks
                .stream()
                .map(Task::name)
                .toList());

        assertEquals(expectedExcludedTasks.size(), actualExcludedTasks.size());
        for (Task task : actualExcludedTasks) {
            assertTrue(expectedExcludedTasks.contains(task.name()));
        }
    }

    protected void taskRoutineCreationTestWithExpectedDuration(String rootTask,
                                                             Function<TaskID, Duration> expectedDuration,
                                                             List<String> expectedSteps) {
        TaskID rootId = tasks.get(rootTask).id();
        RoutineResponse response = routineService.createRoutine(rootId);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(rootId),
                response.routine().estimatedDuration());
    }

    protected void taskRoutineCreationTestWithExpectedDurationAndFilter(String rootTask,
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

    protected void linkRoutineCreationTestWithExpectedDurationAndFilter(Triple<String, String, Integer> rootLink,
                                                                      Function<TaskID, Duration> expectedDuration,
                                                                      TaskNodeTreeFilter filter,
                                                                      List<String> expectedSteps,
                                                                      List<String> excludedTasks) {
        TaskNode node = nodes.get(rootLink);
        RoutineResponse response = routineService.createRoutine(node.linkId(), filter);
        assertRoutine(expectedSteps, response);
        assertEquals(
                expectedDuration.apply(node.child().id()),
                response.routine().estimatedDuration());
        List<LinkID> actualExcludedNodeIds = netDurationService.getHelper(filter).projectChildrenOutsideDurationLimitMap().getOrDefault(node.id(), List.of());

        assertExcludedTasks(excludedTasks, actualExcludedNodeIds);
    }

    protected void taskRoutineCreationTest (String rootTask, List<String> expectedSteps) {
        taskRoutineCreationTestWithExpectedDuration(
                rootTask,
                rootId -> queryService.fetchNetDuration(rootId, null),
                expectedSteps);
    }

    protected void linkRoutineCreationTest(Triple<String, String, Integer> rootLink, List<String> expectedSteps, List<String> excludedTasks)
            throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
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
}
