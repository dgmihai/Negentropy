package com.trajan.negentropy.model.observer;

import com.trajan.negentropy.model.refresh.RefreshStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class TaskObserver extends Observer {
    private final Set<TaskField> modifiedFields = new HashSet<>();

    public void notify(TaskField field) {
        modifiedFields.add(field);
        switch (field) {
            case NAME, DESCRIPTION, REQUIRED, PROJECT, TAGS ->
                    refreshStrategyManager.setRefreshStrategy(RefreshStrategy.TASK);
            case DURATION ->
                    refreshStrategyManager.setRefreshStrategy(RefreshStrategy.ALL_ANCESTORS);
        }
    }
}