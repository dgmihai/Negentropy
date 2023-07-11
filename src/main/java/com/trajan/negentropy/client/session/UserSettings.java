package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.components.TaskTreeGrid;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@SpringComponent
@Accessors(fluent = true)
@Getter
public class UserSettings {
    private final Map<String, Boolean> treeViewColumnVisibility = new HashMap<>();
    private final Map<String, Boolean> routineViewColumnVisibility = new HashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();
    private TaskFilter filter = new TaskFilter();
    @Setter private boolean enableContextMenu = true;

    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting =
            DescriptionViewDefaultSetting.IF_PRESENT;

    public UserSettings() {
        TaskTreeGrid.VISIBILITY_TOGGLEABLE_COLUMNS.forEach(columnKey ->
                treeViewColumnVisibility.put(columnKey, true));

        List<String> hiddenRoutineColumns = List.of(
                TaskTreeGrid.COLUMN_KEY_DRAG_HANDLE,
                TaskTreeGrid.COLUMN_KEY_COMPLETE,
                TaskTreeGrid.COLUMN_KEY_EDIT,
                TaskTreeGrid.COLUMN_KEY_DELETE);

        TaskTreeGrid.VISIBILITY_TOGGLEABLE_COLUMNS.forEach(columnKey ->
                routineViewColumnVisibility.put(columnKey, !hiddenRoutineColumns.contains(columnKey)));
    }
}