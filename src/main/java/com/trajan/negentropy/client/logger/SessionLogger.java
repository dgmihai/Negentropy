package com.trajan.negentropy.client.logger;

import com.trajan.negentropy.util.SpringContext;

public class SessionLogger extends AbstractLogger<SessionPrefixProvider> {

    @Override
    protected SessionPrefixProvider getPrefixProvider() {
        return SpringContext.getBean(SessionPrefixProvider.class);
    }

    public SessionLogger() {
        super();
    }

    public SessionLogger(Shift shift) {
        super(shift);
    }
}
