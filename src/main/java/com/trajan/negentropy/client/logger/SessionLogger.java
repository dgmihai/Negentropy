package com.trajan.negentropy.client.logger;

public class SessionLogger extends AbstractLogger<SessionPrefixProvider> {

    @Override
    protected Class<SessionPrefixProvider> getProviderClass() {
        return SessionPrefixProvider.class;
    }

    @Override
    protected SessionPrefixProvider constructProvider() {
        return new SessionPrefixProvider();
    }

    public SessionLogger() {
        super();
    }

    public SessionLogger(Shift shift) {
        super(shift);
    }
}
