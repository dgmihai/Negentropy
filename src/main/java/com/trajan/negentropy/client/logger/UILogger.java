package com.trajan.negentropy.client.logger;

public class UILogger extends AbstractLogger<UIPrefixProvider> {


    @Override
    protected Class<UIPrefixProvider> getProviderClass() {
        return UIPrefixProvider.class;
    }

    @Override
    protected UIPrefixProvider constructProvider() {
        return new UIPrefixProvider();
    }

    public UILogger() {
        super();
    }

    public UILogger(Shift shift) {
        super(shift);
    }
}
