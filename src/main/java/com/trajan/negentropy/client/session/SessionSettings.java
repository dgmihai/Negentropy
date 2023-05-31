package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.components.tasktreegrid.TaskTreeGrid;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SpringComponent
@Accessors(fluent = true)
@Getter
public class SessionSettings {
    private final Map<String, Boolean> columnVisibility = new HashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();
    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting =
            DescriptionViewDefaultSetting.IF_PRESENT;

    public SessionSettings() {
        TaskTreeGrid.VISIBILITY_TOGGLEABLE_COLUMNS.forEach(columnKey -> {
            columnVisibility.put(columnKey, true);
        });
    }
}