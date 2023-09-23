package com.trajan.negentropy.client.sessionlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionLogger {
    private final Logger log;
    private final String prefix;

    public SessionLogger(String prefix, Class<?> clazz) {
        this.prefix = prefix;
        log = LoggerFactory.getLogger(clazz);
    }

    public SessionLogger(Class<?> clazz) {
        this("[] : ", clazz);
    }

    public void error(String arg0, Object... args) {
        log.error(prefix + arg0, args);
    }

    public void info(String arg0, Object... args) {
        log.info(prefix + arg0, args);
    }

    public void warn(String arg0, Object... args) {
        log.warn(prefix + arg0, args);
    }

    public void debug(String arg0, Object... args) {
        log.debug(prefix + arg0, args);
    }

    public void trace(String arg0, Object... args) {
        log.trace(prefix + arg0, args);
    }
}
