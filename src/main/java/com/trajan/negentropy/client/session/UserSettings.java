package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.session.enums.GridTiling;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter.NestableTaskNodeTreeFilter;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@SpringComponent
@VaadinSessionScope
@Getter
public class UserSettings {
    private final List<LinkedHashMap<ColumnKey, Boolean>> treeViewColumnVisibilities = new ArrayList<>();
    private final LinkedHashMap<ColumnKey, Boolean> routineViewColumnVisibility = new LinkedHashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();

    public static final NestableTaskNodeTreeFilter DEFAULT_FILTER = (NestableTaskNodeTreeFilter) new NestableTaskNodeTreeFilter()
            .completed(false);
    @Setter(AccessLevel.PACKAGE) private NestableTaskNodeTreeFilter filter = DEFAULT_FILTER;
    @Setter private boolean enableContextMenu = true;
    @Setter private SelectionMode gridSelectionMode = SelectionMode.MULTI;
    @Setter private GridTiling gridTiling = GridTiling.NONE;
    @Setter private InsertMode sameGridDragInsertMode = InsertMode.MOVE;
    @Setter private InsertMode differentGridDragInsertMode = InsertMode.MOVE;
    @Setter private boolean routineStepsGridVisible = false;
    @Setter private OnSuccessfulSaveActions onSuccessfulSaveAction = OnSuccessfulSaveActions.CLOSE;
    @Setter private TaskEntry currentRootEntry = null;
    private final String DEFAULT_PROJECT = "Still To Plan";

    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting = DescriptionViewDefaultSetting.IF_PRESENT;

    public UserSettings() {
        treeViewColumnVisibilities.add(new LinkedHashMap<>());
        treeViewColumnVisibilities.add(new LinkedHashMap<>());

        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            for (LinkedHashMap<ColumnKey, Boolean> gridColumnVisibility : treeViewColumnVisibilities) {
                gridColumnVisibility.put(columnKey, (!(
                        columnKey.equals(ColumnKey.TAGS_COMBO) || columnKey.equals(ColumnKey.FROZEN))));
            }
        });

        RoutineStepTreeGrid.possibleColumns.forEach(columnKey -> {
            routineViewColumnVisibility.put(columnKey, true);
        });

        // TODO: Implement focus for both grids
        treeViewColumnVisibilities.get(1).remove(ColumnKey.FOCUS);
    }
}