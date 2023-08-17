package com.trajan.negentropy.model.observer;

import com.trajan.negentropy.model.refresh.RefreshStrategy;
import com.trajan.negentropy.model.refresh.RefreshStrategyManager;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class TaskLinkObserver {
    private Set<TaskLinkField> modifiedFields = new HashSet<>();
    private RefreshStrategyManager refreshStrategyManager = new RefreshStrategyManager();

    public void notify(TaskLinkField field) {
        modifiedFields.add(field);
        switch (field) {
            case POSITION ->
                    refreshStrategyManager.setRefreshStrategy(RefreshStrategy.PARENT_AND_CHILDREN);
            case RECURRING, CRON, IMPORTANCE, PROJECT_DURATION ->
                    refreshStrategyManager.setRefreshStrategy(RefreshStrategy.LINK);
        }
    }

    public RefreshStrategy getRefreshStrategy() {
        return refreshStrategyManager.refreshStrategy();
    }
}
