package com.trajan.negentropy.model.refresh;

public interface HasRefreshStrategy {
    RefreshStrategyManager refreshStrategyManager = new RefreshStrategyManager();

    default RefreshStrategyManager refreshStrategyManager() {
        return this.refreshStrategyManager;
    };
}
