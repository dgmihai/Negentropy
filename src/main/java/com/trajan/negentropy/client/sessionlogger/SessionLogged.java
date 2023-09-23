package com.trajan.negentropy.client.sessionlogger;

public interface SessionLogged {
    SessionLoggerFactory loggerFactory();

    default SessionLogger getLogger(Class<?> clazz) {
        return (loggerFactory() == null)
                ? new SessionLogger(clazz)
                : loggerFactory().getLogger(clazz);
    }
}
