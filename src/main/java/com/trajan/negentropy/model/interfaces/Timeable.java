package com.trajan.negentropy.model.interfaces;

import com.trajan.negentropy.model.entity.TimeableStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public interface Timeable <T extends Timeable<T>> {
    TimeableStatus status();
    T status(TimeableStatus status);
    LocalDateTime startTime();
    T startTime(LocalDateTime startTime);
    LocalDateTime finishTime();
    T finishTime(LocalDateTime finishTime);
    LocalDateTime lastSuspendedTime();
    T lastSuspendedTime(LocalDateTime lastSuspendedTime);
    Duration elapsedSuspendedDuration();
    T elapsedSuspendedDuration(Duration elapsedSuspendedDuration);

    String name();
    String description();
    Duration duration();

    default void complete(LocalDateTime time) {
        this.status(TimeableStatus.COMPLETED);
        if (this.finishTime() == null) this.finishTime(time);
        if (lastSuspendedTime() != null) {
            this.elapsedSuspendedDuration(
                    this.elapsedSuspendedDuration() != null
                            ? this.elapsedSuspendedDuration().plus(Duration.between(this.lastSuspendedTime(), time))
                            : Duration.between(this.lastSuspendedTime(), time));
            this.lastSuspendedTime(null);
        }
    }

    default void start(LocalDateTime time) {
        this.status(TimeableStatus.ACTIVE);
        if (this.startTime() == null) this.startTime(time);
        this.finishTime(null);
        if (lastSuspendedTime() != null) {
            this.elapsedSuspendedDuration(
                    this.elapsedSuspendedDuration() != null
                            ? this.elapsedSuspendedDuration().plus(Duration.between(this.lastSuspendedTime(), time))
                            : Duration.between(this.lastSuspendedTime(), time));
            this.lastSuspendedTime(null);
        }
    }

    default void descendantsStarted(LocalDateTime time) {
        this.status(TimeableStatus.DESCENDANT_ACTIVE);
        this.pause(time);
    }

    default void suspend(LocalDateTime time) {
        this.status(TimeableStatus.SUSPENDED);
        this.pause(time);
    }

    default void skip(LocalDateTime time) {
        this.status(TimeableStatus.SKIPPED);
        this.pause(time);
    }

    default void exclude(LocalDateTime time) {
        this.status(TimeableStatus.EXCLUDED);
        this.finish(time);
    }

    default void postpone(LocalDateTime time) {
        this.status(TimeableStatus.POSTPONED);
        this.finish(time);
    }

    private void finish(LocalDateTime time) {
        if (time != null) {
            if (this.finishTime() != null) this.finishTime(time);
            if (lastSuspendedTime() != null) {
                this.elapsedSuspendedDuration(
                        this.elapsedSuspendedDuration() != null
                                ? this.elapsedSuspendedDuration().plus(Duration.between(this.lastSuspendedTime(), time))
                                : Duration.between(this.lastSuspendedTime(), time));
                this.lastSuspendedTime(null);
            }
        } else {
            this.elapsedSuspendedDuration(null);
            this.lastSuspendedTime(null);
        }
    }

    private void pause(LocalDateTime time) {
        if (this.lastSuspendedTime() == null) this.lastSuspendedTime(time);
        if (this.finishTime() != null) this.finishTime(null);
    }
}