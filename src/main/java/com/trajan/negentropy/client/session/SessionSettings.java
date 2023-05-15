package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.tree.TaskTreeGrid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
@Getter
public class SessionSettings {
    private final Map<String, Boolean> columnVisibility = new HashMap<>();
    private final boolean showAllExistingDescriptions = false;

    public SessionSettings() {
        TaskTreeGrid.VISIBILITY_TOGGLEABLE_COLUMNS.forEach(columnKey -> {
            columnVisibility.put(columnKey, true);
        });
    }
}