package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.grid.subcomponents.NestedTaskTabs;
import com.trajan.negentropy.client.components.routinelimit.StartRoutineDialog;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.GridInlineEditorTaskNodeForm;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.TaskEntryDataProvider;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.cron.ShortenedCronConverter;
import com.trajan.negentropy.client.util.cron.ShortenedCronValueProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.model.sync.Change.CopyChange.CopyType;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoIcon;
import elemental.json.JsonObject;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Getter
@Benchmark
public class TaskEntryTreeGrid extends TaskTreeGrid<TaskEntry> {
    private final UILogger log = new UILogger();

    @Autowired private ShortenedCronValueProvider cronValueProvider;
    @Autowired private TaskNetworkGraph taskNetworkGraph;
    @Autowired private TaskEntryDataProvider taskEntryDataProvider;
    @Autowired private ShortenedCronConverter cronConverter;

    public static final List<ColumnKey> excludedColumns = List.of(
            ColumnKey.STATUS,
            ColumnKey.EXCLUDE,
            ColumnKey.GOTO);

    public static final List<ColumnKey> possibleColumns = Arrays.stream(ColumnKey.values())
            .filter(columnKey -> !excludedColumns.contains(columnKey))
            .toList();

    @Override
    protected MultiSelectTreeGrid<TaskEntry> createGrid() {
        return new MultiSelectTreeGrid<>(TaskEntry.class);
    }

    @Override
    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        super.init(visibleColumns, selectionMode);

        if (settings.enableContextMenu()) {
            log.debug("Enabled context menu");
            new TaskTreeContextMenu(treeGrid);
        }

        this.treeGrid.setDataProvider(taskEntryDataProvider());

        topBar.add(gridOptionsMenu(possibleColumns));
    }

    @Override
    protected void initAdditionalReadColumns(ColumnKey columnKey) {
        // TODO: Generify
        switch (columnKey) {
            case FOCUS -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    GridUtil.inlineVaadinIconLitExpression("expand-full",
                            "expand "))
                                    .withFunction("onClick", t ->
                                            nestedTabs.selectNewRootEntry(t)))
                    .setKey(ColumnKey.FOCUS.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.EXPAND_SQUARE))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case COMPLETE -> {
                Grid.Column<TaskEntry> completedColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("check",
                                                "?active=\"${!item.completed}\" " +
                                                        "?hidden=\"${item.hidden}\""))
                                .withFunction("onClick", entry ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new TaskNode(entry.node().linkId())
                                                        .completed(!entry.node().completed())),
                                        this))
                                .withProperty("completed", entry ->
                                        entry.node().completed())
                                .withProperty("hidden", entry ->
                                        entry.node().recurring()
                                        && entry.node().cron() == null
                                        && !entry.node().completed()))
                        .setKey(ColumnKey.COMPLETE.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.CHECK_SQUARE_O))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                setMultiEditCheckboxHeader(completedColumn,
                        t -> t.node().completed(),
                        (toggle) ->
                                new TaskNodeDTO().completed(toggle),
                        t -> t.node().id());
            }

            case RECURRING -> {
                Grid.Column<TaskEntry> recurringColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("time-forward",
                                                "?active=\"${item.recurring}\" "))
                                .withFunction("onClick", entry -> controller.requestChangeAsync(
                                        new MergeChange<>(
                                                new TaskNode(entry.node().linkId())
                                                        .recurring(!entry.node().recurring()))))
                                .withProperty("recurring", entry ->
                                        entry.node().recurring()))
                        .setKey(ColumnKey.RECURRING.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.TIME_FORWARD))
                        .setAutoWidth(true)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                setMultiEditCheckboxHeader(recurringColumn,
                        t -> t.node().recurring(),
                        (toggle) ->
                                new TaskNodeDTO().recurring(toggle),
                        t -> t.node().id());
            }

            case CYCLE_TO_END -> {
                Grid.Column<TaskEntry> cycleToEndColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("arrow-forward",
                                                "?active=\"${item.cycleToEnd}\" " +
                                                        "?hidden=\"${item.hidden}\""))
                                .withFunction("onClick", entry -> controller.requestChangeAsync(
                                        new MergeChange<>(
                                                new TaskNode(entry.node().linkId())
                                                        .cycleToEnd(!entry.node().cycleToEnd()))))
                                .withProperty("cycleToEnd", entry ->
                                        entry.node().cycleToEnd())
                                .withProperty("hidden", entry ->
                                        !entry.node().recurring()
                                                || entry.node().positionFrozen()
                                                || entry.node().parentId() == null))
                        .setKey(ColumnKey.CYCLE_TO_END.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.ARROW_FORWARD))
                        .setAutoWidth(true)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                setMultiEditCheckboxHeader(cycleToEndColumn,
                        t -> t.node().cycleToEnd(),
                        (toggle) ->
                                new TaskNodeDTO().cycleToEnd(toggle),
                        t -> t.node().id());
            }

            case CRON -> treeGrid.addColumn(entry ->
                            cronValueProvider.apply(entry.node().cron()))
                    .setKey(ColumnKey.CRON.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.CALENDAR_CLOCK))
                    .setWidth(GridUtil.CRON_COL_WIDTH)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            case SCHEDULED_FOR -> treeGrid.addColumn(entry -> {
                        if (entry.node().cron() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                            return formatter.format(entry.node().scheduledFor());
                        } else {
                            return "-- -- -- -- -- -- --";
                        }
                    })
                    .setKey(ColumnKey.SCHEDULED_FOR.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.CALENDAR))
                    .setWidth(GridUtil.DATE_COL_WIDTH)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);

            //        Grid.Column<TaskEntry> priorityColumn = treeGrid
            //                .addColumn(entry ->
            //                        entry.link().getPriority())
            //                .setHeader("Priority")
            //                .setAutoWidth(true)
            //                .setFlexGrow(0);

            case RESCHEDULE_NOW -> {
                Grid.Column<TaskEntry> rescheduleNowColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("backwards",
                                                "?hidden=\"${item.hidden}\""))
                                .withFunction("onClick", entry ->
                                        controller.requestChangeAsync(new OverrideScheduledForChange(
                                                entry.node().linkId(), LocalDateTime.now()),
                                                this))
                                .withProperty("hidden", entry -> entry.node().cron() == null))
                        .setKey(ColumnKey.RESCHEDULE_NOW.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.BACKWARDS))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                // TODO: Add to multi edit header
//                setMultiEditCheckboxHeader(rescheduleNowColumn,
//                        t -> t.task().project(),
//                        (toggle) ->
//                                new Task().project(toggle),
//                        t -> t.task().id());
            }

            case RESCHEDULE_LATER -> {
                Grid.Column<TaskEntry> rescheduleLaterColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("forward",
                                                "?hidden=\"${item.hidden}\""))
                                .withFunction("onClick", entry ->
                                        controller.requestChangeAsync(new OverrideScheduledForChange(
                                                entry.node().linkId(),
                                                        entry.node().cron().next(LocalDateTime.now())),
                                                this))
                                .withProperty("hidden", entry -> entry.node().cron() == null))
                        .setKey(ColumnKey.RESCHEDULE_LATER.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.FORWARD))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);
            }

            case FROZEN -> {
                Grid.Column<TaskEntry> positionFrozenColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                        GridUtil.inlineVaadinIconLitExpression("lock",
                                                "?active=\"${item.positionFrozen}\" " +
                                                        "?hidden=\"${!item.visible}\""))
                                .withFunction("onClick", entry ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                        new TaskNode(entry.node().linkId())
                                                                .positionFrozen(!entry.node().positionFrozen())),
                                                this))
                                .withProperty("positionFrozen", entry ->
                                        entry.node().positionFrozen())
                                .withProperty("visible", entry ->
                                        (entry.task().required() || entry.node().positionFrozen())))
                        .setKey(ColumnKey.FROZEN.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.LOCK))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);
            }

            case DELETE -> treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    GridUtil.inlineVaadinIconLitExpression("trash",
                                    " delete"))
                                    .withFunction("onClick", entry ->
                                            controller.requestChangeAsync(new DeleteChange<>(
                                                    entry.node().linkId()),
                                                    this)))
                    .setKey(ColumnKey.DELETE.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.TRASH))
                    .setWidth(GridUtil.ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }
    }

    @Override
    protected List<ColumnKey> getPossibleColumns() {
        return possibleColumns;
    }

    @Override
    protected void setPartNameGenerator() {
        treeGrid.setPartNameGenerator(entry -> {
            List<String> partNames = new ArrayList<>();

            if (entry.node().completed()) {
                partNames.add(K.GRID_PARTNAME_COMPLETED);
            }
            if (entry.node().scheduledFor().isAfter(LocalDateTime.now())) {
                partNames.add(K.GRID_PARTNAME_FUTURE);
            }
            if (entry.task().project()) {
                partNames.add(K.GRID_PARTNAME_PROJECT);
            }
            if (entry.node().recurring()) {
                partNames.add(K.GRID_PARTNAME_RECURRING);
            }
            if (entry.task().difficult()) {
                partNames.add(K.GRID_PARTNAME_DIFFICULT);
            }
            if (entry.node().parentId() != null && settings.areNetDurationsVisible()) {
                List<LinkID> childrenExceedingDurationLimit = taskNetworkGraph.netDurationInfo().get().projectChildrenOutsideDurationLimitMap().get(entry.parent().node().id());
                if (childrenExceedingDurationLimit != null && childrenExceedingDurationLimit.contains(entry.node().linkId())) {
                    partNames.add(K.GRID_PARTNAME_DURATION_LIMIT_EXCEEDED);
                }
            }

            return Joiner.on(" ").join(partNames);
        });
    }

    @Override
    protected GridInlineEditorTaskNodeForm<TaskEntry> getTaskFormLayout(TaskEntry entry) {
        return new GridInlineEditorTaskNodeForm<>(controller, entry, TaskEntry.class);
    }

    @Override
    protected Binder<TaskEntry> setEditorBinder(AbstractTaskFormLayout form) {
        GridInlineEditorTaskNodeForm<TaskEntry> teForm = (GridInlineEditorTaskNodeForm<TaskEntry>) form;
        return teForm.binder();
    }

    @Override
    protected Registration setEditorSaveListener() {
        return editor.addSaveListener(e -> controller.requestChangesAsync(List.of(
                new MergeChange<>(e.getItem().task()),
                new MergeChange<>(e.getItem().node())),
                this));
    }

    @Override
    protected void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (visibleColumns.getOrDefault(ColumnKey.DRAG_HANDLE, false)) {
            treeGrid.addDropListener(event -> {
                log.debug(draggedItem + " dropped onto " +
                        event.getDropTargetItem().orElseThrow().task().name());
                if (event.getDropTargetItem().isPresent()) {
                    TaskEntry target = event.getDropTargetItem().get();
                    if (!draggedItem.task().id().equals(target.task().id())) {
                        InsertMode operation;
                        if (event.getSource().equals(this.treeGrid)) {
                            operation = settings.sameGridDragInsertMode();
                        } else {
                            operation = settings.differentGridDragInsertMode();
                        }

                        Consumer<InsertLocation> changeSupplier = switch (operation) {
                            case MOVE -> location -> controller.requestChangeAsync(new MoveChange(
                                    draggedItem.node().linkId(),
                                    target.node().linkId(),
                                    location));
                            case ADD -> location -> controller.requestChangeAsync(new InsertAtChange(
                                    draggedItem.node().toDTO(),
                                    target.node().linkId(),
                                    location));
                            case SHALLOW_COPY -> location -> controller.requestChangeAsync(new CopyChange(
                                    CopyType.SHALLOW,
                                    draggedItem.node().linkId(),
                                    target.node().linkId(),
                                    location,
                                    controller.settings().filter(),
                                    " (copy)"));
                            case DEEP_COPY -> location -> controller.requestChangeAsync(new CopyChange(
                                    CopyType.DEEP,
                                    draggedItem.node().linkId(),
                                    target.node().linkId(),
                                    location,
                                    controller.settings().filter(),
                                    " (copy)"));
                        };

                        switch (event.getDropLocation()) {
                            case ABOVE -> changeSupplier.accept(InsertLocation.BEFORE);
                            case BELOW -> changeSupplier.accept(InsertLocation.AFTER);
                            case ON_TOP -> changeSupplier.accept(InsertLocation.CHILD);
                        }
                    } else {
                        NotificationMessage.error("Cannot move or copy item onto itself");
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
//        Consumer<TaskEntry> detailsVisibilitySwitch = entry -> {
//            switch (settings.descriptionViewDefaultSetting()) {
//                case ALL ->
//                        controller.taskEntryDataProvider().taskTaskEntriesMap().values().forEach(set ->
//                                set.forEach(ntry ->
//                                        treeGrid.setDetailsVisible(ntry, true)));
//                case IF_PRESENT ->
//                        controller.taskEntryDataProvider().taskTaskEntriesMap().values().forEach(set ->
//                                set.forEach(ntry -> {
//                            if (!entry.task().description().isBlank()) {
//                                treeGrid.setDetailsVisible(ntry, true);
//                            }
//                        }));
//                case NONE ->
//                        controller.taskEntryDataProvider().taskTaskEntriesMap().values().forEach(set ->
//                                set.forEach(ntry ->
//                                        treeGrid.setDetailsVisible(ntry, false)));
//            }
//        };

//        treeGrid.addExpandListener(event -> {
//            Collection<TaskEntry> entries = event.getItems();
//            entries.forEach(detailsVisibilitySwitch);
//        });

        treeGrid.getDataProvider().addDataProviderListener(event ->
                treeGrid.expand(settings.expandedEntries()));
    }

    protected Collection<Component> configureAdditionalTopBarComponents() {
        List<Component> components = new LinkedList<>();
        if (visibleColumns.containsKey(ColumnKey.FOCUS) && visibleColumns.get(ColumnKey.FOCUS)) {
            this.nestedTabs = new NestedTaskTabs(this);

            DoubleClickListenerUtil.add(treeGrid, entry ->
                    nestedTabs.selectNewRootEntry(entry));

            components.add(nestedTabs);
        }
        return components;
    }

    @Override
    protected FormLayout configureMiddleBar() {
        Grid.Column<TaskEntry> cronColumn = treeGrid.getColumnByKey(ColumnKey.CRON.toString());
        Grid.Column<TaskEntry> scheduledForColumn = treeGrid.getColumnByKey(ColumnKey.SCHEDULED_FOR.toString());

        if (cronColumn != null || scheduledForColumn != null) {
            CronTextField cronField = new CronTextField().small();
            Icon check = LumoIcon.CHECKMARK.create();
            check.setSize("2em");
            check.addClassName(K.ICON_COLOR_PRIMARY);

            check.addClickListener(event -> {
                Set<TaskEntry> data = treeGrid.getSelectedItems();
                try {
                    if (!cronField.getValue().isBlank()) {
                        CronExpression cron = cronConverter.convertToModel(cronField.getValue());
                        controller.requestChangeAsync(new MultiMergeChange<>(
                                new TaskNodeDTO()
                                        .cron(cron),
                                data.stream()
                                        .map(entry -> entry.node().id())
                                        .distinct()
                                        .toList()));
                    }
                } catch (IllegalArgumentException e) {
                    NotificationMessage.error(e);
                }
            });

            HorizontalLayout cronHeaderLayout = new HorizontalLayout(cronField, check);
            cronHeaderLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            cronField.setWidth("6em");
            cronHeaderLayout.setSpacing(false);
            cronHeaderLayout.setPadding(false);
            editHeaderLayout.add(cronHeaderLayout);
        }

        if (editHeaderLayout.getElement().getChildCount() > 0) {
            Span label = new Span("Edit selected:");
            editHeaderLayout.addComponentAsFirst(label);
            treeGrid.addSelectionListener(e -> label.setText("Edit selected: " + treeGrid.getSelectedItems().size()
                    + " items"));

            editHeaderLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 3),
                    new FormLayout.ResponsiveStep(K.MEDIUM_SCREEN_WIDTH, 6));

            return editHeaderLayout;
        }

        return editHeaderLayout;
    }

    @Override
    public Optional<TaskNode> rootNode() {
        return controller.taskEntryDataProvider().rootEntry() != null
                ? Optional.of(controller.taskEntryDataProvider().rootEntry().node())
                : Optional.empty();
    }

    // ================================================================================================================
    // TaskTreeContextMenu
    // ================================================================================================================

    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {
        private final TreeGrid<TaskEntry> grid;

        private void addInsertItem(GridSubMenu<TaskEntry> activeTaskSubMenu, String label, InsertLocation location) {
            activeTaskSubMenu.addItem(label, e -> e.getItem().ifPresent(
                    entry -> {
                        Change taskPersist = Change.update(controller.activeTaskNodeProvider().getTask());
                        controller.requestChangesAsync(List.of(
                                taskPersist,
                                new ReferencedInsertAtChange(
                                        (TaskNodeDTO) controller.activeTaskNodeProvider().getNodeInfo(),
                                        entry.node().id(),
                                        location,
                                        taskPersist.id())));
                    })
            );
        }

        private void addMoveOrCopySelectedItem(GridSubMenu<TaskEntry> copySelectedSubMenu,
                                               GridSubMenu<TaskEntry> moveSelectedSubMenu,
                                               Map<String, InsertLocation> moveOrCopyOptions) {
            moveOrCopyOptions.forEach( (label, location) -> {
                int reverse = (location == InsertLocation.AFTER || location == InsertLocation.FIRST) ? -1 : 1;
                List<GridSubMenu<TaskEntry>> subMenus = List.of(copySelectedSubMenu, moveSelectedSubMenu);
                subMenus.forEach(menu -> menu.addItem(label, e -> {
                    controller.requestChangesAsync(grid.getSelectedItems().stream()
                            .collect(Collectors.groupingBy(
                                    entry -> Objects.requireNonNullElse(
                                            entry.node().parentId(), TaskID.nil()),
                                    Collectors.collectingAndThen(
                                            Collectors.toList(),
                                            list -> list.stream()
                                                    .sorted(Comparator.comparingInt(a ->
                                                            reverse * a.node().position()))
                                                    .collect(Collectors.toList()))
                            )).values().stream()
                            .flatMap(List::stream)
                            .map(entry -> menu.equals(copySelectedSubMenu)
                                    ? new InsertAtChange(new TaskNodeDTO(entry.node()),
                                    e.getItem().get().node().id(),
                                    location)
                                    : new MoveChange(entry.node().linkId(),
                                    e.getItem().get().node().id(),
                                    location))
                            .toList());

                    grid.getSelectionModel().deselectAll();
                }));
            });
        }

        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
            super(grid);
            this.grid = grid;

            GridMenuItem<TaskEntry> activeTask = addItem("");
            Hr activeTaskHr = new Hr();
            GridSubMenu<TaskEntry> activeTaskSubMenu = activeTask.getSubMenu();

            GridMenuItem<TaskEntry> startRoutine = addItem("Start Routine",
                    e -> e.getItem().ifPresent(entry -> SpringContext.getBean(StartRoutineDialog.class)
                            .open(List.of(entry.node()))));

            GridMenuItem<TaskEntry> startRoutineSelected = addItem("Start Routine from selected",
                    e -> SpringContext.getBean(StartRoutineDialog.class).open(
                            treeGrid.getSelectedItems().stream()
                                    .map(entry -> (PersistedDataDO) entry.node())
                                    .toList()));

            GridMenuItem<TaskEntry> resetSchedule = addItem("Reset Schedule", e -> e.getItem().ifPresent(
                    entry -> controller.requestChangeAsync(new OverrideScheduledForChange(
                            entry.node().linkId(), LocalDateTime.now()))));

            addInsertItem(activeTaskSubMenu, "Before", InsertLocation.BEFORE);
            addInsertItem(activeTaskSubMenu, "After", InsertLocation.AFTER);
            addInsertItem(activeTaskSubMenu, "As First Subtask", InsertLocation.FIRST);
            addInsertItem(activeTaskSubMenu, "As Last Subtask", InsertLocation.LAST);

            add(new Hr());

            GridMenuItem<TaskEntry> moveTarget = addItem("");
            GridSubMenu<TaskEntry> moveTargetSubMenu = moveTarget.getSubMenu();

            BiConsumer<String, InsertLocation> addMoveTargetItem = (label, location) -> {
                moveTargetSubMenu.addItem(label, e -> e.getItem().ifPresent(
                            entry -> controller.requestChangeAsync(new MoveChange(
                                    entry.node().linkId(),
                                    entry.parent().node().id(),
                                    location))));
            };

            addMoveTargetItem.accept("To Top", InsertLocation.FIRST);
            addMoveTargetItem.accept("To Bottom", InsertLocation.LAST);

            GridMenuItem<TaskEntry> moveSelected = addItem("");
            GridSubMenu<TaskEntry> moveSelectedSubMenu = moveSelected.getSubMenu();
            GridMenuItem<TaskEntry> copySelected = addItem("");
            GridSubMenu<TaskEntry> copySelectedSubMenu = copySelected.getSubMenu();

            Map<String, InsertLocation> moveOrCopyOptions = new HashMap<>();
            moveOrCopyOptions.put("Before", InsertLocation.BEFORE);
            moveOrCopyOptions.put("After", InsertLocation.AFTER);
            moveOrCopyOptions.put("As Subtasks At Front", InsertLocation.FIRST);
            moveOrCopyOptions.put("As Subtasks At Back", InsertLocation.LAST);

            addMoveOrCopySelectedItem(copySelectedSubMenu, moveSelectedSubMenu, moveOrCopyOptions);

            add(new Hr());

            GridMenuItem<TaskEntry> remove = addItem("Remove");

            GridMenuItem<TaskEntry> confirmRemove = remove.getSubMenu().addItem("Confirm?", e -> e.getItem().ifPresent(
                    entry -> controller.requestChangeAsync(new DeleteChange<>(entry.node().linkId()))));
            confirmRemove.addClassName(K.COLOR_ERROR);
            remove.addClassName(K.COLOR_ERROR);

            add(new Hr());

            GridMenuItem<TaskEntry> multiEdit = addItem("");
            GridSubMenu<TaskEntry> multiEditSubMenu = multiEdit.getSubMenu();

            GridMenuItem<TaskEntry> completed = multiEditSubMenu.addItem("Set Completed To");
            GridMenuItem<TaskEntry> recurring = multiEditSubMenu.addItem("Set Recurring To");
            GridMenuItem<TaskEntry> required = multiEditSubMenu.addItem("Set Required To");
            GridMenuItem<TaskEntry> delete = multiEditSubMenu.addItem("Delete?");

            Consumer<Boolean> requestCompletedChange = isCompleted -> {
                controller.requestChangeAsync(new MultiMergeChange<>(
                        new TaskNodeDTO().completed(isCompleted),
                        treeGrid.getSelectedItems().stream()
                                .map(entry -> entry.node().id())
                                .toList()));
            };

            completed.getSubMenu().addItem("True", e -> requestCompletedChange.accept(true));
            completed.getSubMenu().addItem("False", e -> requestCompletedChange.accept(false));

            Consumer<Boolean> requestRecurringChange = isRecurring -> {
                controller.requestChangeAsync(new MultiMergeChange<>(
                        new TaskNodeDTO().recurring(isRecurring),
                        treeGrid.getSelectedItems().stream()
                                .map(entry -> entry.node().id())
                                .toList()));
            };

            recurring.getSubMenu().addItem("True", e -> requestRecurringChange.accept(true));
            recurring.getSubMenu().addItem("False", e -> requestRecurringChange.accept(false));

            Consumer<Boolean> requestRequiredChange = isRequired -> {
                controller.requestChangeAsync(new MultiMergeChange<>(
                        new Task().required(isRequired),
                        treeGrid.getSelectedItems().stream()
                                .map(entry -> entry.task().id())
                                .toList()));
            };

            required.getSubMenu().addItem("True", e -> requestRequiredChange.accept(true));
            required.getSubMenu().addItem("False", e -> requestRequiredChange.accept(false));
            
            delete.getSubMenu().addItem("Confirm", e -> controller.requestChangesAsync(
                    treeGrid.getSelectedItems().stream()
                            .map(entry -> (Change) new DeleteChange<>(entry.node().linkId()))
                            .toList()));

            // Do not show context menu when header is clicked
            setDynamicContentHandler(entry -> {
                if (entry == null) {
                    return false;
                } else {
                    boolean hasActiveTaskProvider =
                            controller.activeTaskNodeProvider() != null &&
                                    controller.activeTaskNodeProvider().isValid();

                    if (hasActiveTaskProvider) {
                        try {
                            Task task = controller.activeTaskNodeProvider().getTask();
                            if (task != null) {
                                activeTask.setText("Insert " + task.name());
                                activeTask.setVisible(true);
                                activeTaskHr.setVisible(true);
                            }
                        } catch (Exception e) {
                            NotificationMessage.error(e);
                        }
                    } else {
                        activeTask.setVisible(false);
                        activeTaskHr.setVisible(false);
                    }

                    List<TaskEntry> selected = List.copyOf(grid.getSelectedItems());
                    int selectedSize = selected.size();
                    multiEdit.setVisible(selectedSize > 1);
                    if (selectedSize == 0) {
                        moveTarget.setVisible(true);
                        moveSelected.setVisible(false);
                        copySelected.setVisible(false);
                        startRoutineSelected.setVisible(false);
                        moveTarget.setText("Move " + entry.task().name());
                    } else {
                        moveTarget.setVisible(false);
                        moveSelected.setVisible(true);
                        String multiSelectText = selectedSize == 1
                                ? selected.get(0).task().name()
                                : selected.get(0).task().name() + " (+" + (selectedSize - 1) + " more)";
                        moveSelected.setText("Move " + multiSelectText);
                        copySelected.setVisible(true);
                        copySelected.setText("Copy " + multiSelectText);
                        startRoutineSelected.setVisible(true);
                        startRoutineSelected.setText("Start Routine from " + multiSelectText);
                        multiEdit.setText("Edit " + multiSelectText);
                    }

                    activeTaskSubMenu.getItems().forEach(menuItem -> menuItem.setEnabled(hasActiveTaskProvider));

                    startRoutine.setEnabled(true);
                    startRoutine.setText("Start Routine from " + entry.task().name());

                    remove.setEnabled(true);
                    remove.setText("Remove " + entry.task().name());
                    resetSchedule.setEnabled(entry.node().cron() != null);

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
