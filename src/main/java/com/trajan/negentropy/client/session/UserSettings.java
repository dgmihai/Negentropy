package com.trajan.negentropy.client.session;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
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
    private final List<LinkedHashMap<String, Boolean>> treeViewColumnVisibilities = new ArrayList<>();
    private final LinkedHashMap<String, Boolean> routineViewColumnVisibility = new LinkedHashMap<>();
    private final Set<TaskEntry> expandedEntries = new HashSet<>();
    @Setter private TaskFilter filter = new TaskFilter();
    @Setter private boolean enableContextMenu = true;
    @Setter private GridTiling gridTiling = GridTiling.NONE;

    @Setter
    private DescriptionViewDefaultSetting descriptionViewDefaultSetting =
            DescriptionViewDefaultSetting.IF_PRESENT;

    public UserSettings() {
        treeViewColumnVisibilities.add(new LinkedHashMap<>());
        treeViewColumnVisibilities.add(new LinkedHashMap<>());

        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            for (LinkedHashMap<String, Boolean> gridColumnVisibility : treeViewColumnVisibilities) {
                gridColumnVisibility.put(columnKey, true);

                if (columnKey.equals(K.COLUMN_KEY_DRAG_HANDLE)) {
                    gridColumnVisibility.put(K.COLUMN_KEY_NAME, true);
                }
            }
        });
        // TODO: Implement focus for both grids
        treeViewColumnVisibilities.get(1).remove(K.COLUMN_KEY_FOCUS);

        List<String> hiddenRoutineColumns = List.of(
                K.COLUMN_KEY_COMPLETE,
                K.COLUMN_KEY_EDIT,
                K.COLUMN_KEY_DELETE);

        treeViewColumnVisibilities.forEach(visibility -> visibility.put(K.COLUMN_KEY_NAME, true));
        TaskEntryTreeGrid.possibleColumns.forEach(columnKey -> {
            if (!hiddenRoutineColumns.contains(columnKey)) {
                routineViewColumnVisibility.put(columnKey, true);
            }
        });
    }

    @Getter
    public enum GridTiling {
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal"),
        NONE("None");

        private final String value;

        GridTiling(String value) {
            this.value = value;
        }

        public static Optional<GridTiling> get(String string) {
            return Arrays.stream(GridTiling.values())
                    .filter(env -> env.value.equals(string))
                    .findFirst();
        }
    }
}