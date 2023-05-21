package com.trajan.negentropy.client.routine.components;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.shared.Registration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Tag("simple-clock")
@JsModule("./simple-clock/simple-clock.js")
public class SimpleClock extends Component implements HasSize, HasStyle, Serializable {

    private static final long serialVersionUID = 1L;
    private static final int START_TIME_S = 0;
    private static final String DISPLAY = "display";
    private static final String INLINE = "inline";
    private static final String CURRENT_TIME = "currentTime";
    private static final String START_TIME = "startTime";
    private static final String SHOW_ETA = "showETA";

    /** Creates a timer with a start time of 0 */
    public SimpleClock() {
        this(START_TIME_S);
    }

    /**
     * Creates an upwards counting timer using the start time passed in the constructor
     *
     * @param startTime value in seconds for the start time
     */
    public SimpleClock(final Number startTime) {
        getElement().getStyle().set(DISPLAY, INLINE);
        setStartTime(startTime);
    }

    /**
     * Sets the start time
     *
     * @param startTime value in seconds for the start time
     */
    public void setStartTime(final Number startTime) {
        getElement().setProperty(START_TIME, startTime.doubleValue());
        getElement().setProperty(CURRENT_TIME, startTime.doubleValue());
        reset();
    }

    /**
     * Sets whether output is displayed as ETA or in duration format
     *
     * @param showEta whether the output is formatted as ETA or not
     */
    public void setShowEta(final boolean showEta) {
        getElement().setProperty(SHOW_ETA, showEta);
    }

    /** Gets whether output is displayed as ETA or in duration format */
    public boolean showETA() {
        return Boolean.parseBoolean(this.getElement().getProperty(SHOW_ETA));
    }

    /**
     * Starts or stops the timer depending on an input
     *
     * @param run whether to play or pause the timer
     */
    public void run(boolean run) {
        if (run) {
            this.play();
        } else {
            this.pause();
        }
    }

    /** Starts or stops the timer if it is already started */
    public void play() {
        getElement().callJsFunction("play");
    }

    /** Stops the timer, does nothing if already stopped */
    public void pause() {
        getElement().callJsFunction("pause");
    }

    /** Resets the current value to the start time */
    public void reset() {
        getElement().callJsFunction("ready");
    }

    /**
     * Returns the status of the timer
     *
     * @return
     */
    @Synchronize(property = "isRunning", value = "is-running-changed")
    public boolean isRunning() {
        return getElement().getProperty("isRunning", false);
    }

    /**
     * Returns the last known value of the timer. The value is updated when the
     * CurrentTimeChangeListener executes.
     *
     * @return current value in seconds
     */
    @Synchronize("is-running-changed")
    public BigDecimal getCurrentTime() {
        return BigDecimal.valueOf(getElement().getProperty(CURRENT_TIME, 0d));
    }

    /**
     * Returns the current value of the timer.
     *
     * @return a pending result that completes after retrieving the timer value.
     */
    public CompletableFuture<BigDecimal> getCurrentTimeAsync() {
        return getElement()
                .executeJs("return this.currentTime")
                .toCompletableFuture(Double.class)
                .thenApply(BigDecimal::valueOf);
    }

    /**
     * Adds a property change listener for the {@code currentTime} property
     *
     * @return current value in seconds
     */
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
}
