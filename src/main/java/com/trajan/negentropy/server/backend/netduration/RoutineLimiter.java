package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.util.SpringContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
@Getter
@Setter
public class RoutineLimiter {

    @ToString.Include
    private final Duration durationLimit;
    private Duration durationSum = Duration.ZERO;
    @ToString.Include
    private final Integer countLimit;
    private Integer count = 0;
    @ToString.Include
    private final LocalDateTime etaLimit;
    @ToString.Include
    private final boolean customLimit;
    @ToString.Include
    private boolean exceeded = false;

    public boolean isEmpty() {
        return durationLimit == null && countLimit == null && etaLimit == null && !exceeded;
    }

    public boolean wouldExceed(Duration duration) {
        if (exceeded) return true;

        log.trace("Potential shift: " + duration);
        Duration potential = durationSum.plus(duration);
        log.trace("Potential duration sum: " + potential);
        log.trace("Duration limit: " + durationLimit);

        if (durationLimit != null && durationLimit.compareTo(potential) < 0) {
            log.trace("Would exceed duration limit of " + durationLimit + " with " + potential);
            return true;
        }

        if (countLimit != null && count + 1 > countLimit) {
            log.trace("Would exceed count limit");
            return true;
        }

        boolean wouldExceedEta = etaExceedsLimit(duration);
        if (wouldExceedEta) log.trace("Would exceed eta limit");
        return wouldExceedEta;
    }

    public void include (Duration duration, boolean required) {
        if (duration != null) include(List.of(duration), required);
    }

    public void include (Collection<Duration> durations, boolean required) {
        Duration shift = durations.stream()
                .reduce(Duration.ZERO, Duration::plus);
        log.trace("Shift: " + shift);
        durationSum = durationSum.plus(shift);
        log.trace("Duration sum: " + durationSum);
        log.trace("Duration limit: " + durationLimit);

        int countShift = required ? 0 : durations.size();
        if (shift.isNegative()) {
            count -= countShift;
        } else {
            count += countShift;
        }

        if (durationLimit != null && durationLimit.compareTo(durationSum) < 0) {
            log.trace("Exceeded duration limit of " + durationLimit + " with " + durationSum);
            exceeded = true;
            return;
        }

        if (countLimit != null && count > countLimit) {
            log.trace("Exceeded count limit");
            exceeded = true;
            return;
        }

        exceeded = etaExceedsLimit();

        if (exceeded) log.trace("Exceeded eta limit");
    }

    private boolean etaExceedsLimit() {
        return etaExceedsLimit(Duration.ZERO);
    }

    private boolean etaExceedsLimit(Duration duration) {
        log.trace("Checking eta limit: " + etaLimit);
        if (etaLimit != null) {
            // Needed for testing
            RoutineService routineService = SpringContext.getBean(RoutineService.class);
            LocalDateTime potentialEta = routineService.now().plus(durationSum).plus(duration);
            boolean result = potentialEta.isAfter(etaLimit);
            log.trace("ETA limit: " + etaLimit + " vs potential: " + potentialEta + " = " + result);
            return result;
        } else {
            return false;
        }
    }
}