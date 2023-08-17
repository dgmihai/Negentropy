package com.trajan.negentropy.model.refresh;

import lombok.Getter;

@Getter
public class RefreshStrategyManager {
    private RefreshStrategy refreshStrategy;

    public RefreshStrategyManager() {
        this.refreshStrategy = RefreshStrategy.NONE;
    }

    public void setRefreshStrategy(RefreshStrategy newStrategy) {
        if (newStrategy.getLevel() > refreshStrategy.getLevel()) {
            refreshStrategy = newStrategy;
        }
    }
}
