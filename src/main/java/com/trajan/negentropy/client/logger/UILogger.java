package com.trajan.negentropy.client.logger;

import com.trajan.negentropy.util.SpringContext;

public class UILogger extends AbstractLogger<UIPrefixProvider> {

    @Override
    protected UIPrefixProvider getPrefixProvider() {
        return SpringContext.getBean(UIPrefixProvider.class);
    }

    public UILogger() {
        super();
    }

    public UILogger(Shift shift) {
        super(shift);
    }
}
