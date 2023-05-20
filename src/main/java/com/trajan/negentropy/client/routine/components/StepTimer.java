package com.trajan.negentropy.client.routine.components;

import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.backend.entity.status.StepStatus;
import com.trajan.negentropy.server.facade.model.RoutineStep;
import com.trajan.negentropy.util.RoutineUtil;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

@Accessors(fluent = true)
@Getter
public class StepTimer extends SimpleTimer {
    private static final Logger logger = LoggerFactory.getLogger(StepTimer.class);
    private static final String CURRENT_TIME = "currentTime";

    private RoutineStep step;

    private final ClientDataController controller;

    public StepTimer(RoutineStep step, ClientDataController controller) {
        super();
        this.controller = controller;

        super.setMinutes(true);
        super.setFractions(false);

        this.setStep(step);
    }

    @Override
    public void setStartTime(final Number startTime) {
        getElement().setProperty("startTime", startTime.doubleValue());
    }

    public void setCurrentTime(final Number currentTime) {
        getElement().setProperty(CURRENT_TIME, currentTime.doubleValue());
    }

    @Override
    public void setCountUp(final boolean countUp) {
        logger.debug("Counting Up");
        getElement().setProperty("countUp", countUp);
    }

    public void setStep(RoutineStep step) {
        this.reset();
        this.step = step;

        Duration remainingDuration = RoutineUtil.getRemainingRoutineStepDuration(step, LocalDateTime.now());
        logger.debug("Remaining Duration: " + remainingDuration);

        Number remainingSeconds = remainingDuration.abs().toSeconds();

        Runnable countingUp = () -> {
            this.setCountUp(true);
            this.setStartTime(Duration.ofDays(1).toSeconds());
            this.setCurrentTime(remainingSeconds.doubleValue());
        };

        if (remainingDuration.isNegative()) {
            countingUp.run();
            this.addClassNames(K.COLOR_ERROR);
        } else if (remainingDuration.isZero()) {
            countingUp.run();
        } else {
            this.setCountUp(false);
            this.setStartTime(remainingSeconds);
            this.setCurrentTime(remainingSeconds.doubleValue());
            countingUp.run();
        }

        this.addTimerEndEvent(event -> {
            countingUp.run();
        });

        if (step.status().equals(StepStatus.ACTIVE)) {
            this.start();
        }
    }

    @Override
    public void pause() {
        super.pause();
        controller.pauseRoutineStep(step.id());
    }

    @Override
    public void start() {
        super.start();
        controller.startRoutineStep(step.id());
    }
}