package com.trajan.negentropy.model.interfaces;

import com.trajan.negentropy.model.entity.TimeableStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface Timeable {
//    ID id();
    String name();
    String description();
    Duration duration();
    LocalDateTime startTime();
    LocalDateTime finishTime();
    TimeableStatus status();
}