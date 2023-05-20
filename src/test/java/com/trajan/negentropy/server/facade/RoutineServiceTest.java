package com.trajan.negentropy.server.facade;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.backend.entity.status.StepStatus;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoutineServiceTest extends TaskTestTemplate {

    @Autowired private RoutineService routineService;

    @BeforeAll
    void setup() {
        init();
    }

    private void assertRoutine(List<String> expectedSteps, RoutineResponse response) {
        assertTrue(response.success());

        Routine routine = response.routine();

        assertTrue(Iterables.elementsEqual(expectedSteps, routine.steps().stream()
                .map(step -> step.task().name())
                .peek(System.out::println).toList()));

        for (RoutineStep step : routine.steps()) {
            assertEquals(StepStatus.NOT_STARTED, step.status());
        }
    }

    @Test
    void testCreateRoutine() {
        TaskID rootId = tasks.get(TWOTWO).id();

        RoutineResponse response = routineService.createRoutine(rootId);

        List<String> expectedSteps = List.of(
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO
        );

        assertRoutine(expectedSteps, response);
        assertEquals(
                entityQueryService.getTotalDuration(rootId).totalDuration(),
                response.routine().estimatedDuration());
    }

    @Test
    void testExecuteRoutine() {
        TaskID rootId = tasks.get(TWOTWO).id();

        Routine routine = routineService.createRoutine(rootId).routine();

        RoutineStep rootStep = routine.steps().get(0);

        assertEquals(rootId, rootStep.task().id());

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.steps().get(routine.currentPosition()).task().name());
        assertEquals(StepStatus.NOT_STARTED, routine.currentStep().status());
        assertEquals(RoutineStatus.NOT_STARTED, routine.status());

        RoutineResponse response = routineService.startStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(0, routine.currentPosition());
        assertEquals(TWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());
        assertEquals(RoutineStatus.ACTIVE, routine.status());

        response = routineService.completeStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(1, routine.currentPosition());
        assertEquals(TWOTWOONE, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());
        assertEquals(StepStatus.COMPLETED, routine.steps().get(routine.currentPosition() - 1).status());

        // TODO: ETA & Duration timing
//        assertEquals(
//                entityQueryService.getTotalDuration(rootId).totalDuration()
//                        .minus(Duration.ofMinutes(0)),
//                response.routine().estimatedDuration());

        response = routineService.skipStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());

        // TODO: ETA & Duration timing
//        assertEquals(
//                entityQueryService.getTotalDuration(rootId).totalDuration()
//                        .minus(Duration.ofMinutes(1)),
//                response.routine().estimatedDuration());

        response = routineService.suspendStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.SUSPENDED, routine.currentStep().status());
        assertEquals(StepStatus.SKIPPED, routine.steps().get(routine.currentPosition() - 1).status());

        response = routineService.startStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());

        response = routineService.completeStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());
        assertEquals(StepStatus.COMPLETED, routine.steps().get(routine.currentPosition() - 1).status());

        response = routineService.previousStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(2, routine.currentPosition());
        assertEquals(TWOTWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());
        assertEquals(StepStatus.SUSPENDED, routine.steps().get(routine.currentPosition() + 1).status());

        response = routineService.skipStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.ACTIVE, routine.currentStep().status());
        assertEquals(StepStatus.SKIPPED, routine.steps().get(routine.currentPosition() - 1).status());

        response = routineService.completeStep(routine.currentStep().id(), LocalDateTime.now());
        assertTrue(response.success());
        routine = response.routine();

        assertEquals(3, routine.currentPosition());
        assertEquals(TWOTWOTHREE_AND_THREETWOTWO, routine.currentStep().task().name());
        assertEquals(StepStatus.COMPLETED, routine.currentStep().status());
        assertEquals(StepStatus.SKIPPED, routine.steps().get(routine.currentPosition() - 1).status());
        assertEquals(RoutineStatus.COMPLETED, routine.status());
    }
}