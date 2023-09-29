package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.shared.Registration;
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

    @Override
    public void onEnabledStateChanged(boolean enabled) {
        super.onEnabledStateChanged(enabled);
        fireEvent(new TaskProviderTabEvent(this, false, enabled));
    }

    public Registration addEnabledStateChangeListener(
            ComponentEventListener<TaskProviderTabEvent> listener) {
        return ComponentUtil.addListener(this, TaskProviderTabEvent.class, listener);
    }

    @Getter
    public static class TaskProviderTabEvent extends ComponentEvent<TaskProviderTab> {
        private final boolean newEnabledState;

        public TaskProviderTabEvent(TaskProviderTab source, boolean fromClient, boolean newEnabledState) {
            super(source, fromClient);
            this.newEnabledState = newEnabledState;
        }
    }
}