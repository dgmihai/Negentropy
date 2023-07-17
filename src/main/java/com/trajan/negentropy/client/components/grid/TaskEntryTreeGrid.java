package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.components.NestedTaskTabs;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.TaskNodeDataFormLayout;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.routine.RoutineView;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.cron.CronValueProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import elemental.json.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Slf4j
@Accessors(fluent = true)
@Getter
public class TaskEntryTreeGrid extends TaskTreeGrid<TaskEntry> {
    @Autowired private CronValueProvider cronValueProvider;

    public static List<String> possibleColumns = List.of(
            K.COLUMN_KEY_DRAG_HANDLE,
            K.COLUMN_KEY_FOCUS,
            K.COLUMN_KEY_BLOCK,
            K.COLUMN_KEY_PROJECT,
            K.COLUMN_KEY_COMPLETE,
            K.COLUMN_KEY_RECURRING,
            K.COLUMN_KEY_CRON,
            K.COLUMN_KEY_SCHEDULED_FOR,
            K.COLUMN_KEY_TAGS,
            K.COLUMN_KEY_DESCRIPTION,
            K.COLUMN_KEY_DURATION,
            K.COLUMN_KEY_TIME_ESTIMATE,
            K.COLUMN_KEY_EDIT,
            K.COLUMN_KEY_DELETE);

    @Override
    protected TreeGrid<TaskEntry> createGrid() {
        return new TreeGrid<>(TaskEntry.class);
    }

    @Override
    public void init(LinkedHashMap<String, Boolean> visibleColumns) {
        super.init(visibleColumns);

        if (settings.enableContextMenu()) {
            log.debug("Enabled context menu");
            new TaskTreeContextMenu(treeGrid);
        }

        topBar.add(gridOptionsMenu(possibleColumns));
    }

    @Override
    protected void initAdditionalReadColumns(String column) {
        // TODO: Generify
        switch (column) {
            case K.COLUMN_KEY_FOCUS -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                    inlineVaadinIconLitExpression("expand-full",
                            "expand "))
                                    .withFunction("onClick", t ->
                                            nestedTabs.onSelectNewRootEntry(t)))
                    .setKey(K.COLUMN_KEY_FOCUS)
                    .setHeader(headerIcon(VaadinIcon.EXPAND_SQUARE))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case K.COLUMN_KEY_PROJECT -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("file-tree",
                                            "?active=\"${item.project}\" "))
                            .withFunction("onClick", entry ->
                                    controller.updateTask(entry.task()
                                            .project(!entry.task().project())))
                            .withProperty("project", entry ->
                                    entry.task().project()))
                    .setKey(K.COLUMN_KEY_PROJECT)
                    .setHeader(headerIcon(VaadinIcon.FILE_TREE))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case K.COLUMN_KEY_COMPLETE -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                    inlineVaadinIconLitExpression("check",
                            "?active=\"${!item.completed}\" "))
                                    .withFunction("onClick", entry ->
                                            controller.updateNode(entry.node()
                                                    .completed(!entry.node().completed())))
                                    .withProperty("completed", entry ->
                                            entry.node().completed()))
                    .setKey(K.COLUMN_KEY_COMPLETE)
                    .setHeader(headerIcon(VaadinIcon.CHECK_SQUARE_O))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case K.COLUMN_KEY_RECURRING ->
                treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                        inlineVaadinIconLitExpression("time-forward",
                                "?active=\"${item.recurring}\" "))
                                        .withFunction("onClick", entry ->
                                                controller.updateNode(entry.node()
                                                        .recurring(!entry.node().recurring())))
                                        .withProperty("recurring", entry ->
                                                entry.node().recurring()))
                    .setKey(K.COLUMN_KEY_RECURRING)
                    .setHeader(headerIcon(VaadinIcon.TIME_FORWARD))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case K.COLUMN_KEY_CRON -> treeGrid.addColumn(entry ->
                            cronValueProvider.apply(entry.node().cron()))
                   .setKey(K.COLUMN_KEY_CRON)
                   .setHeader(headerIcon(VaadinIcon.CALENDAR_CLOCK))
                   .setAutoWidth(false)
                   .setFlexGrow(0)
                   .setTextAlign(ColumnTextAlign.CENTER);

            case K.COLUMN_KEY_SCHEDULED_FOR -> treeGrid.addColumn(entry -> {
                        if (entry.node().cron() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                            return formatter.format(entry.node().scheduledFor());
                        } else {
                            return "-- -- -- -- -- -- --";
                        }
                    })
                    .setKey(K.COLUMN_KEY_SCHEDULED_FOR)
                    .setHeader(headerIcon(VaadinIcon.CALENDAR))
                    .setAutoWidth(false)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            //        Grid.Column<TaskEntry> priorityColumn = treeGrid
            //                .addColumn(entry ->
            //                        entry.link().getPriority())
            //                .setHeader("Priority")
            //                .setAutoWidth(true)
            //                .setFlexGrow(0);

            case K.COLUMN_KEY_DELETE -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                    inlineVaadinIconLitExpression("trash",
                            "?hidden=\"${item.isHidden}\" " +
                                    "delete"))
                                    .withFunction("onClick", entry ->
                                            controller.deleteNode(entry))
                                    .withProperty("isHidden", entry ->
                                            entry.task().hasChildren()))
                    .setKey(K.COLUMN_KEY_DELETE)
                    .setHeader(headerIcon(VaadinIcon.TRASH))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }
    }

    @Override
    protected void setPartNameGenerator() {
        treeGrid.setPartNameGenerator(entry -> {
            List<String> partNames = new ArrayList<>();

            if (entry.node().completed()) {
                partNames.add(K.GRID_PARTNAME_COMPLETED);
            }
            if (entry.task().block()) {
                partNames.add(K.GRID_PARTNAME_BLOCK);
            }
            if (entry.task().project()) {
                partNames.add(K.GRID_PARTNAME_PROJECT);
            }

            return Joiner.on(" ").join(partNames);
        });
    }

//    @Override
//    protected void setColumnOrder() {
//        treeGrid.setColumnOrder(
//                dragHandleColumn,
//                nameColumn,
//                focusColumn,
//                blockColumn,
//                completeColumn,
//                recurringColumn,
//                cron_column,
//                scheduled_for_column,
//                tagColumn,
//                descriptionColumn,
//                taskDurationColumn,
//                timeEstimateColumn,
//                editColumn,
//                deleteColumn
//        );
//    }

    @Override
    protected TaskNodeDataFormLayout<TaskEntry> getTaskFormLayout(TaskEntry entry) {
        return new TaskNodeDataFormLayout<>(controller, entry, TaskEntry.class);
    }

    @Override
    protected Binder<TaskEntry> setEditorBinder(AbstractTaskFormLayout form) {
        TaskNodeDataFormLayout<TaskEntry> teForm = (TaskNodeDataFormLayout) form;
        return teForm.binder();
    }

    @Override
    protected Registration setEditorSaveListener() {
        return editor.addSaveListener(e -> controller.updateEntry(e.getItem()));
    }

    @Override
    protected void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (visibleColumns.getOrDefault(K.COLUMN_KEY_DRAG_HANDLE, false)) {
            treeGrid.addDropListener(event -> {
                log.debug(draggedItem + " dropped onto " +
                        event.getDropTargetItem().orElseThrow().task().name());
                if (event.getDropTargetItem().isPresent()) {
                    TaskEntry target = event.getDropTargetItem().get();
                    if (!draggedItem.task().id().equals(target.task().id())) {
                        if (event.getSource().equals(this.treeGrid)) {
                            switch (event.getDropLocation()) {
                                case ABOVE -> controller.moveNodeBefore(
                                        draggedItem,
                                        target);
                                case BELOW -> controller.moveNodeAfter(
                                        draggedItem,
                                        target);
                                case ON_TOP -> controller.moveNodeInto(
                                        draggedItem,
                                        target);
                            }
                        } else {
                            switch (event.getDropLocation()) {
                                case ABOVE -> controller.copyNodeBefore(
                                        draggedItem,
                                        target);
                                case BELOW -> controller.copyNodeAfter(
                                        draggedItem,
                                        target);
                                case ON_TOP -> controller.copyNodeInto(
                                        draggedItem,
                                        target);
                            }
                        }
                    } else {
                        NotificationError.show("Cannot move or copy item onto itself");
                    }
                }
                draggedItem = null;
                treeGrid.setRowsDraggable(false);
            });
        }
    }

    @Override
    protected void configureAdditionalEvents() {
        treeGrid.addExpandListener(event ->
                settings.expandedEntries().addAll(event.getItems()));

        treeGrid.addCollapseListener(event ->
                settings.expandedEntries().removeAll(event.getItems()));

        // TODO: Needs an in-memory TreeDataProvider to actually work
        Consumer<TaskEntry> detailsVisibilitySwitch = entry -> {
            switch (settings.descriptionViewDefaultSetting()) {
                case ALL ->
                        controller.dataProvider().getCachedTaskEntriesByChildTaskId().values().forEach(set ->
                                set.forEach(ntry ->
                                        treeGrid.setDetailsVisible(ntry, true)));
                case IF_PRESENT ->
                        controller.dataProvider().getCachedTasks().values().forEach(task -> {
                            if (!task.description().isBlank()) {
                                controller.dataProvider().getCachedTaskEntriesByChildTaskId().get(task.id()).forEach(ntry ->
                                    treeGrid.setDetailsVisible(ntry, true)
                                );
                            }
                        });
                case NONE ->
                        controller.dataProvider().getCachedTaskEntriesByChildTaskId().values().forEach(set ->
                                set.forEach(ntry ->
                                        treeGrid.setDetailsVisible(ntry, false)));
            }
        };

//        treeGrid.addExpandListener(event -> {
//            Collection<TaskEntry> entries = event.getItems();
//            entries.forEach(detailsVisibilitySwitch);
//        });

        treeGrid.getDataProvider().addDataProviderListener(event -> {
            treeGrid.expand(settings.expandedEntries());
        });
    }

    protected Collection<Component> configureAdditionalTobBarComponents() {
        if (visibleColumns.containsKey(K.COLUMN_KEY_FOCUS) && visibleColumns.get(K.COLUMN_KEY_FOCUS)) {
            this.nestedTabs = new NestedTaskTabs(controller);

            DoubleClickListenerUtil.add(treeGrid, entry ->
                    nestedTabs.onSelectNewRootEntry(entry));

            return List.of(nestedTabs);
        }

        return List.of();
    }

    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {

        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
            super(grid);

            GridMenuItem<TaskEntry> activeTask = addItem("");
            GridSubMenu<TaskEntry> activeTaskSubMenu = activeTask.getSubMenu();

            GridMenuItem<TaskEntry> startRoutine = addItem("Start Routine", e -> e.getItem().ifPresent(
                    entry -> {
                        controller.createRoutine(entry.task().id());
                        UI.getCurrent().navigate(RoutineView.class);
                    }
            ));

            GridMenuItem<TaskEntry> insertBefore = activeTaskSubMenu.addItem("Before", e -> e.getItem().ifPresent(
                    controller::addTaskFromActiveProviderBefore
            ));

            GridMenuItem<TaskEntry> insertAfter = activeTaskSubMenu.addItem("After", e -> e.getItem().ifPresent(
                    controller::addTaskFromActiveProviderAfter
            ));

            GridMenuItem<TaskEntry> insertAsSubtask = activeTaskSubMenu.addItem("As subtask", e -> e.getItem().ifPresent(
                    controller::addTaskFromActiveProviderAsChild
            ));

            add(new Hr());

            addItem("Remove", e -> e.getItem().ifPresent(
                    controller::deleteNode
            ));

            // Do not show context menu when header is clicked
            setDynamicContentHandler(entry -> {
                if (entry == null) {
                    return false;
                } else {
                    boolean hasActiveTaskProvider =
                            controller.activeTaskProvider() != null &&
                                    controller.activeTaskProvider().hasValidTask().success();

                    if (hasActiveTaskProvider) {
                        try {
                            controller.activeTaskProvider().getTask().ifPresentOrElse(
                                    task -> activeTask.setText("Insert " + task.name()), () -> {
                                    });
                            activeTask.setVisible(true);
                        } catch (Exception e) {
                            NotificationError.show(e);
                        }
                    } else {
                        activeTask.setText("No valid task to add");
                        activeTask.setVisible(false);
                    }

                    insertBefore.setEnabled(hasActiveTaskProvider);
                    insertAfter.setEnabled(hasActiveTaskProvider);
                    insertAsSubtask.setEnabled(hasActiveTaskProvider);
                    return true;
                }
            });
        }

        @Override
        protected boolean onBeforeOpenMenu(JsonObject eventDetail) {
            if (eventDetail.getString("columnId").equals(K.COLUMN_ID_DRAG_HANDLE)) {
                return false;
            }
            return super.onBeforeOpenMenu(eventDetail);
        }
    }


}
