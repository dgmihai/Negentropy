package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag("countdown-timer")
@JsModule("./simple-clock/countdown-timer.js")
public class CountdownTimer extends AbstractTimer {

    public CountdownTimer(Timeable timeable) {
        super(timeable);
    }

    @Override
    public void setTimeable(Timeable timeable) {
        super.setTimeable(timeable);

        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);

        Duration remainingDuration = timeableUtil.getRemainingDuration(timeable, LocalDateTime.now());
        Number remainingSeconds = remainingDuration.negated().toSeconds();
        this.setStartTime(remainingSeconds);

        boolean isActive = timeable.status().equals(TimeableStatus.ACTIVE);
        this.play();
    }
}
