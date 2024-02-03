package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag("countdown-timer")
@JsModule("./simple-clock/countdown-timer.js")
public class CountdownTimer extends AbstractTimer<RoutineStep> {

    public CountdownTimer(RoutineStep step) {
        super(step);
    }

    @Override
    public void setTimeable(RoutineStep step) {
        super.setTimeable(step);

        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);

        Duration remainingDuration = step.children().isEmpty()
                ? timeableUtil.getRemainingDuration(step, LocalDateTime.now())
                : timeableUtil.getRemainingNestedDuration(step, LocalDateTime.now(), true);
        Number remainingSeconds = remainingDuration.negated().toSeconds();
        this.setStartTime(remainingSeconds);

        if (step.status().equals(TimeableStatus.ACTIVE)) {
            this.play();
        } else {
            this.pause();
        }
    }
}
