package com.trajan.negentropy.model.observer;

import com.trajan.negentropy.model.refresh.RefreshStrategyManager;

public abstract class Observer {
    protected RefreshStrategyManager refreshStrategyManager = new RefreshStrategyManager();
}
