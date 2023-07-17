package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.controller.data.HasTaskProvider;
import com.trajan.negentropy.client.controller.data.TaskProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent=true)
public class TaskProviderTab extends Tab implements HasTaskProvider {
    private final TaskProvider taskProvider;

    public TaskProviderTab(TaskProvider taskProvider, String label) {
        super(label);
        this.taskProvider = taskProvider;
    }

    public TaskProviderTab(TaskProvider taskProvider, Component... components) {
        super(components);
        this.taskProvider = taskProvider;
    }
}