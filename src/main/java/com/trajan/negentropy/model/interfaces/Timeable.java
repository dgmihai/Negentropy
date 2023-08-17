package com.trajan.negentropy.model.interfaces;

import com.trajan.negentropy.model.entity.TimeableStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface Timeable {
    String name();
    String description();
    Duration remainingDuration(LocalDateTime time);
    TimeableStatus status();
}