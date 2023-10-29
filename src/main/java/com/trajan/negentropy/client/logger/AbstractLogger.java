package com.trajan.negentropy.client.logger;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

import java.lang.StackWalker.StackFrame;

public abstract class AbstractLogger <T extends PrefixProvider> {
    protected final Logger log;
    protected String prefix;
    protected static final String EMPTY_PREFIX = "[] :";

    protected T prefixProvider;

    @Setter
    protected SessionLogger.Shift shift = SessionLogger.Shift.NONE;

    public enum Shift {
        DOWN,
        NONE,
        UP
    }

    protected abstract T getPrefixProvider();

    public AbstractLogger() {
        this.setPrefix();
        log = LoggerFactory.getLogger(getCaller());
    }

    public AbstractLogger(SessionLogger.Shift shift) {
        this.setPrefix();
        this.shift = shift;
        log = LoggerFactory.getLogger(getCaller());
    }

    protected void setPrefix() {
        this.prefixProvider = getPrefixProvider();
        try {
            this.prefix = prefixProvider.prefix();
        } catch (BeansException e) {
            this.prefix = EMPTY_PREFIX;
        }
    }

    private String getCaller() {
        return StackWalker.getInstance()
                .walk(frames -> frames
                        .skip(3)
                        .findFirst()
                        .map(StackFrame::getClassName)
                        .orElse("?"));
    }

    private String formatMessage(String message) {
        return prefix + message;
    }

    public void error(String arg0, Object... args) {
        log.error(formatMessage(arg0), args);
    }

    public void info(String arg0, Object... args) {
        switch (shift) {
            case NONE, UP -> log.info(formatMessage(arg0), args);
            case DOWN -> log.debug(formatMessage(arg0), args);
        }
    }

    public void warn(String arg0, Object... args) {
        log.warn(formatMessage(arg0), args);
    }

    public void debug(String arg0, Object... args) {
        switch (shift) {
            case NONE -> log.debug(formatMessage(arg0), args);
            case UP -> log.info(formatMessage(arg0), args);
            case DOWN -> log.trace(formatMessage(arg0), args);
        }
    }

    public void trace(String arg0, Object... args) {
        switch (shift) {
            case NONE, DOWN -> log.trace(formatMessage(arg0), args);
            case UP -> log.debug(formatMessage(arg0), args);
        }
    }
}
