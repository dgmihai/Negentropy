package com.trajan.negentropy.server.rest;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/routines")
@Slf4j
@Benchmark
public class RoutineRestController {

    @Autowired private RoutineService routineService;

    @GetMapping("/routines")
    public ResponseEntity<List<Routine>> getRoutines(@RequestParam Set<TimeableStatus> statuses) {
        Stream<Routine> routines = routineService.fetchRoutines(statuses);
        return ResponseEntity.ok(routines.toList());
    }

    @GetMapping("/step/{stepId}")
    public ResponseEntity<RoutineStep> getRoutineStep(@PathVariable StepID stepId) {
        RoutineStep routineStep = routineService.fetchRoutineStep(stepId);
        return ResponseEntity.ok(routineStep);
    }

    @PostMapping("/step/{stepId}/start")
    public RoutineResponse startStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.startStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/suspend")
    public RoutineResponse suspendStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.suspendStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/complete")
    public RoutineResponse completeStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.completeStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/skip")
    public RoutineResponse skipStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.skipStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/postpone")
    public RoutineResponse postponeStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.postponeStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/exclude")
    public RoutineResponse excludeStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.excludeStep(stepId, time);
    }

    @PostMapping("/step/{stepId}/previous")
    public RoutineResponse previousStep(
            @PathVariable StepID stepId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime time) {
        return routineService.previousStep(stepId, time);
    }
}

