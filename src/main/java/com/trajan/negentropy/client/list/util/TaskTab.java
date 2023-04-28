package com.trajan.negentropy.client.list.util;

import com.trajan.negentropy.client.TaskEntry;
import com.vaadin.flow.component.tabs.Tab;
import lombok.Getter;

@Getter
public class TaskTab extends Tab {
    private final TaskEntry entry;

    public TaskTab(TaskEntry entry) {
        super(entry.node().child().name());
        this.entry = entry;
    }
}
