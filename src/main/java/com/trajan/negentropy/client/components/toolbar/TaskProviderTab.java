package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent=true)
public class TaskProviderTab extends Tab implements HasTaskNodeProvider {
    private final HasTaskNodeProvider taskNodeProviderOwner;

    public TaskProviderTab(HasTaskNodeProvider taskProvider, String label) {
        super(label);
        this.taskNodeProviderOwner = taskProvider;
    }

    public TaskProviderTab(HasTaskNodeProvider taskProvider, Component... components) {
        super(components);
        this.taskNodeProviderOwner = taskProvider;
    }

    public TaskNodeProvider taskNodeProvider() {
        return taskNodeProviderOwner.taskNodeProvider();
    }
}