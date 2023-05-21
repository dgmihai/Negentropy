package com.trajan.negentropy.client.routine.components;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.model.Timeable;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

@Accessors(fluent = true)
@Getter
public class RoutineTimer extends SimpleClock {
    private static final Logger logger = LoggerFactory.getLogger(RoutineTimer.class);

    private Timeable timeable;

    private final ClientDataController controller;

    public RoutineTimer(Timeable timeable, ClientDataController controller) {
        super();
        this.controller = controller;

        this.setTimeable(timeable);
    }

    public void setTimeable(Timeable timeable) {
        this.reset();
        this.timeable = timeable;

        Duration remainingDuration = timeable.remainingDuration(LocalDateTime.now());
        Number remainingSeconds = remainingDuration.negated().toSeconds();
        this.setStartTime(remainingSeconds);

        boolean isActive = timeable.status().equals(TimeableStatus.ACTIVE);

        this.run(isActive ^ this.showETA());
    }
}