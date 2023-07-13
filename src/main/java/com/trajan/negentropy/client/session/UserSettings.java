package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@SpringComponent
@Accessors(fluent = true)
@Getter
public class UserSettings {
    private final LinkedHashMap<String, Boolean> treeViewColumnVisibility = new LinkedHashMap<>();
    private final LinkedHashMap<String, Boolean> routineViewColumnVisibility = new LinkedHashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();
    private TaskFilter filter = new TaskFilter();
    @Setter private boolean enableContextMenu = true;

    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting =
            DescriptionViewDefaultSetting.IF_PRESENT;

    public UserSettings() {
        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            treeViewColumnVisibility.put(columnKey, true);

            if (columnKey.equals(K.COLUMN_KEY_DRAG_HANDLE)) {
                treeViewColumnVisibility.put(K.COLUMN_KEY_NAME, true);
            }
        });

        List<String> hiddenRoutineColumns = List.of(
                K.COLUMN_KEY_COMPLETE,
                K.COLUMN_KEY_EDIT,
                K.COLUMN_KEY_DELETE);

        routineViewColumnVisibility.put(K.COLUMN_KEY_NAME, true);
        routineViewColumnVisibility.put(K.COLUMN_KEY_STATUS, true);
        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            if (!hiddenRoutineColumns.contains(columnKey)) {
                routineViewColumnVisibility.put(columnKey, true);
            }
        });
    }
}