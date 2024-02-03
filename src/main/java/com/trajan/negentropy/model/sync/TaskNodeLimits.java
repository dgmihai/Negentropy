package com.trajan.negentropy.model.sync;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

public record TaskNodeLimits(Optional<Duration> duration, Optional<LocalTime> time, Optional<Integer> stepCount) {}
