package com.trajan.negentropy.client.components.routine;

import com.trajan.negentropy.model.interfaces.Timeable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.shared.Registration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public abstract class AbstractTimer extends Component implements HasSize, HasStyle, Serializable {
    private static final long serialVersionUID = 1L;
    protected static final String DISPLAY = "display";
    protected static final String INLINE = "inline";
    protected static final String CURRENT_TIME = "currentTime";
    protected static final String START_TIME = "startTime";

    protected Timeable timeable;

    public AbstractTimer(Timeable timeable) {
        getElement().getStyle().set(DISPLAY, INLINE);
        setTimeable(timeable);
    }

    public void setStartTime(final Number startTime) {
        getElement().setProperty(START_TIME, startTime.doubleValue());
        getElement().setProperty(CURRENT_TIME, startTime.doubleValue());
        reset();
    }

    public void play() {
        getElement().callJsFunction("play");
    }

    public void pause() {
        getElement().callJsFunction("pause");
    }

    public void reset() {
        getElement().callJsFunction("ready");
    }

    @Synchronize(property = "isRunning", value = "is-running-changed")
    public boolean isRunning() {
        return getElement().getProperty("isRunning", false);
    }

    @Synchronize("is-running-changed")
    public BigDecimal getCurrentTime() {
        return BigDecimal.valueOf(getElement().getProperty(CURRENT_TIME, 0d));
    }

    public Registration addCurrentTimeChangeListener(
            PropertyChangeListener listener, long time, TimeUnit timeUnit) {
        int millis = (int) Math.min(timeUnit.toMillis(time), Integer.MAX_VALUE);
        if (listener == null) {
            listener = ev -> {};
        }
        return getElement()
                .addPropertyChangeListener(CURRENT_TIME, "current-time-changed", listener)
                .throttle(millis);
    }

    @Override
    public boolean isVisible() {
        return getStyle().get(DISPLAY).equals(INLINE);
    }

    @Override
    public void setVisible(boolean visible) {
        getStyle().set(DISPLAY, visible ? INLINE : "none");
    }

    public void setTimeable(Timeable timeable) {
        this.reset();
        this.timeable = timeable;    }
}
