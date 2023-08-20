package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.components.grid.ColumnKey;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.session.enums.GridTiling;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

@SpringComponent
@Accessors(fluent = true)
@Getter
public class UserSettings {
    private final List<LinkedHashMap<ColumnKey, Boolean>> treeViewColumnVisibilities = new ArrayList<>();
    private final LinkedHashMap<ColumnKey, Boolean> routineViewColumnVisibility = new LinkedHashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();

    @Setter private TaskFilter filter = new TaskFilter();
    @Setter private boolean enableContextMenu = true;
    @Setter private boolean multiSelect = true;
    @Setter private GridTiling gridTiling = GridTiling.NONE;
    @Setter private InsertMode sameGridDragInsertMode = InsertMode.MOVE;
    @Setter private InsertMode differentGridDragInsertMode = InsertMode.MOVE;
    @Setter private boolean routineStepsGridVisible = true;
    @Setter private OnSuccessfulSaveActions onSuccessfulSaveAction = OnSuccessfulSaveActions.CLOSE;

    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting = DescriptionViewDefaultSetting.IF_PRESENT;

    public UserSettings() {
        treeViewColumnVisibilities.add(new LinkedHashMap<>());
        treeViewColumnVisibilities.add(new LinkedHashMap<>());

        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            for (LinkedHashMap<ColumnKey, Boolean> gridColumnVisibility : treeViewColumnVisibilities) {
                gridColumnVisibility.put(columnKey, (!columnKey.equals(ColumnKey.TAGS_COMBO)));
            }
        });

        RoutineStepTreeGrid.possibleColumns.forEach(columnKey -> {
            routineViewColumnVisibility.put(columnKey, true);
        });

        // TODO: Implement focus for both grids
        treeViewColumnVisibilities.get(1).remove(ColumnKey.FOCUS);
    }
}