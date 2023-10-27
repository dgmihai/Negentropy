package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.time.Duration;
import java.time.LocalDateTime;

@Tag("eta-timer")
@JsModule("./simple-clock/eta-timer.js")
public class ETATimer extends AbstractTimer {

    public ETATimer(Timeable timeable) {
        super(timeable);
    }

    @Override
    public void setTimeable(Timeable timeable) {
        super.setTimeable(timeable);

        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);

        Duration remainingDuration;
        if (timeable instanceof RoutineStep step) {
            remainingDuration = timeableUtil.getRemainingNetDuration(step, LocalDateTime.now());
        } else {
            remainingDuration = timeableUtil.getRemainingDuration(timeable, LocalDateTime.now());
        }

        this.setStartTime(remainingDuration.toSeconds());

        this.play();
    }
}
