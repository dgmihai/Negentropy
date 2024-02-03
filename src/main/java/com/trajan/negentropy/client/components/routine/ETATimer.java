package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

@Tag("eta-timer")
@JsModule("./simple-clock/eta-timer.js")
public class ETATimer extends Component implements HasSize, HasStyle, Serializable {
    private final UILogger log = new UILogger();

    private static final String DISPLAY = "display";
    private static final String INLINE = "inline";
    private static final String NET_DURATION = "netDuration";
    private static final String IS_ACTIVE = "isActive";
    private static final String FORMATTED_TIME = "formattedTime";

    public ETATimer(RoutineStep step, Routine routine) {
        this.getElement().getStyle().set(DISPLAY, INLINE);
        this.setTimeable(step, routine);
    }

    public void ready() {
        this.getElement().callJsFunction("ready");
    }

    public void setTimeable(RoutineStep step, Routine routine) {
        LocalDateTime now = LocalDateTime.now();
        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);
        Duration netDuration = timeableUtil.getRemainingNetDuration(step, now);
        log.trace("netDuration: " + netDuration);
        boolean isActive = routine.currentStep().status().equals(TimeableStatus.ACTIVE);
        log.trace("isActive: " + isActive);

        getElement().setProperty(NET_DURATION, netDuration.toMillis());
        getElement().setProperty(IS_ACTIVE, isActive);

        this.ready();
    }
}
