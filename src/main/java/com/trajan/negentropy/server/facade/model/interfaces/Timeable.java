package com.trajan.negentropy.server.facade.model.interfaces;

import com.trajan.negentropy.server.backend.entity.TimeableStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface Timeable {
    String name();
    String description();
    Duration remainingDuration(LocalDateTime time);
    TimeableStatus status();
}