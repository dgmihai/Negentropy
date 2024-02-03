package com.trajan.negentropy.server.rest;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineNodeStep;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.function.Function;

@RestController
@RequestMapping("/routine/active")
@Slf4j
@Benchmark
public class SimpleRoutineRestController {
    @Autowired private RoutineService routineService;

    private ResponseEntity<RoutineStep> makeCall(Function<StepID, RoutineResponse> call) {
        if (routineService.activeRoutineId() != null) {
            StepID currentStepId = routineService.fetchRoutine(routineService.activeRoutineId()).currentStep().id();
            RoutineStep resultStep = call.apply(currentStepId).routine().currentStep();
            Routine resultRoutine = routineService.fetchRoutine(resultStep.routineId());
            if (!resultRoutine.status().equals(TimeableStatus.ACTIVE)) {
                RoutineStep completionStep = new RoutineNodeStep()
                        .status(resultRoutine.status());
                completionStep.node().task()
                        .name(resultRoutine.status().toString())
                        .description(resultRoutine.name());
                return ResponseEntity.ok(completionStep);
            } else {
                return ResponseEntity.ok(resultStep);
            }
        } else {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    @GetMapping("")
    public ResponseEntity<Routine> currentRoutine() {
        if (routineService.activeRoutineId() != null) {
            return ResponseEntity.ok(routineService.fetchRoutine(routineService.activeRoutineId()));
        } else {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    @GetMapping("/step")
    public ResponseEntity<RoutineStep> current() {
        if (routineService.activeRoutineId() != null) {
            StepID currentStepId = routineService.fetchRoutine(routineService.activeRoutineId()).currentStep().id();
            RoutineStep resultStep = routineService.fetchRoutineStep(currentStepId);
            return ResponseEntity.ok(resultStep);
        } else {
            return ResponseEntity.notFound()
                    .build();
        }
    }

    @PostMapping("/next")
    public ResponseEntity<RoutineStep> next() {
        log.debug("Received REST request to go to next step");
        return makeCall(id -> routineService.completeStep(id, LocalDateTime.now()));
    }

    @PostMapping("/skip")
    public ResponseEntity<RoutineStep> skip() {
        log.debug("Received REST request to skip step");
        return makeCall(id -> routineService.skipStep(id, LocalDateTime.now()));
    }

    @PostMapping("/previous")
    public ResponseEntity<RoutineStep> previous() {
        log.debug("Received REST request to go to previous step");
        return makeCall(id -> routineService.previousStep(id, LocalDateTime.now()));
    }

    @PostMapping("/pause")
    public ResponseEntity<RoutineStep> pause() {
        log.debug("Received REST request to pause step");
        return makeCall(id -> routineService.suspendStep(id, LocalDateTime.now()));
    }

    @PostMapping("/go")
    public ResponseEntity<RoutineStep> go() {
        log.debug("Received REST request to activate step");
        return makeCall(id -> routineService.startStep(id, LocalDateTime.now()));
    }

    @PostMapping("/exclude")
    public ResponseEntity<RoutineStep> exclude() {
        log.debug("Received REST request to exclude step");
        return makeCall(id -> routineService.excludeStep(id, LocalDateTime.now()));
    }

    @PostMapping("/postpone")
    public ResponseEntity<RoutineStep> postpone() {
        log.debug("Received REST request to postpone step");
        return makeCall(id -> routineService.postponeStep(id, LocalDateTime.now()));
    }

    // Sue me
    private static final LinkID DIAPER_CHANGE = new LinkID(29114);
    @PostMapping("/diaper")
    public ResponseEntity<RoutineStep> diaper() {
        log.debug("REST request to start diaper change");
        RoutineStep step = makeCall(id -> routineService.createRoutine(DIAPER_CHANGE, LocalDateTime.now())).getBody();
        return makeCall(id -> routineService.startStep(step.id(), LocalDateTime.now()));
    }
}
