package com.trajan.negentropy.client.components.grid;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.CronTextField;
import com.trajan.negentropy.client.components.grid.subcomponents.NestedTaskTabs;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.GridInlineEditorTaskNodeFormLayout;
import com.trajan.negentropy.client.controller.TaskNetworkGraph;
import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager;
import com.trajan.negentropy.client.controller.dataproviders.TaskEntryDataProviderManager.TaskEntryDataProvider;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.routine.RoutineView;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.cron.ShortenedCronConverter;
import com.trajan.negentropy.client.util.cron.ShortenedCronValueProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.model.sync.Change.CopyChange.CopyType;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import elemental.json.JsonObject;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Accessors(fluent = true)
@Getter
public class TaskEntryTreeGrid extends TaskTreeGrid<TaskEntry> {
    @Autowired private ShortenedCronValueProvider cronValueProvider;
    @Autowired private TaskNetworkGraph taskNetworkGraph;
    @Autowired private TaskEntryDataProviderManager dataProviderManager;
    @Autowired private ShortenedCronConverter cronConverter;
    private TaskEntryDataProvider gridDataProvider;

    protected static final String CRON_WIDTH = "120px";
    protected static final String DATE_WIDTH = "120px";

    public static final List<ColumnKey> excludedColumns = List.of(
            ColumnKey.STATUS,
            ColumnKey.EXCLUDE,
            ColumnKey.GOTO);

    public static final List<ColumnKey> possibleColumns = Arrays.stream(ColumnKey.values())
            .filter(columnKey -> !excludedColumns.contains(columnKey))
            .toList();

    @PostConstruct
    public void init() {
        log = getLogger(this.getClass());
    }

    @Override
    protected TreeGrid<TaskEntry> createGrid() {
        return new TreeGrid<>(TaskEntry.class);
    }

    @Override
    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        super.init(visibleColumns, selectionMode);

        if (settings.enableContextMenu()) {
            log.debug("Enabled context menu");
            new TaskTreeContextMenu(treeGrid);
        }

        this.gridDataProvider = dataProviderManager.create();
        dataProviderManager.allProviders().add(gridDataProvider);
        this.treeGrid.setDataProvider(gridDataProvider);

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
                                            nestedTabs.onSelectNewRootEntry(t)))
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
                                .withFunction("onClick", entry -> {
                                    controller.requestChangeAsync(new MergeChange<>(
                                            new TaskNode(entry.node().linkId())
                                                    .completed(!entry.node().completed())),
                                            this);
                                })
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
                                .withFunction("onClick", entry -> {
                                    controller.requestChangeAsync(new MergeChange<>(
                                            new TaskNode(entry.node().linkId())
                                                    .recurring(!entry.node().recurring())));
                                })
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

            case CRON -> treeGrid.addColumn(entry ->
                                cronValueProvider.apply(entry.node().cron()))
                        .setKey(ColumnKey.CRON.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.CALENDAR_CLOCK))
                        .setWidth(CRON_WIDTH)
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
                    .setWidth(DATE_WIDTH)
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
                                .withFunction("onClick", entry -> {
                                    controller.requestChangeAsync(new OverrideScheduledForChange(
                                            entry.node().linkId(), LocalDateTime.now()),
                                            this);
                                })
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
                                .withFunction("onClick", entry -> {
                                    controller.requestChangeAsync(new OverrideScheduledForChange(
                                            entry.node().linkId(),
                                            entry.node().cron().next(LocalDateTime.now())),
                                            this);
                                })
                                .withProperty("hidden", entry -> entry.node().cron() == null))
                        .setKey(ColumnKey.RESCHEDULE_LATER.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.FORWARD))
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
            if (entry.task().required()) {
                partNames.add(K.GRID_PARTNAME_REQUIRED);
            }
            if (entry.task().project()) {
                partNames.add(K.GRID_PARTNAME_PROJECT);
            }
            if (entry.node().recurring()) {
                partNames.add(K.GRID_PARTNAME_RECURRING);
            }
            if (entry.node().parentId() != null) {
                List<LinkID> childrenExceedingDurationLimit = taskNetworkGraph.netDurationInfo().projectChildrenOutsideDurationLimitMap().get(entry.parent().node().id());
                if (childrenExceedingDurationLimit != null && childrenExceedingDurationLimit.contains(entry.node().linkId())) {
                    partNames.add(K.GRID_PARTNAME_DURATION_LIMIT_EXCEEDED);
                }
            }

            return Joiner.on(" ").join(partNames);
        });
    }

    @Override
    protected GridInlineEditorTaskNodeFormLayout<TaskEntry> getTaskFormLayout(TaskEntry entry) {
        return new GridInlineEditorTaskNodeFormLayout<>(controller, entry, TaskEntry.class);
    }

    @Override
    protected Binder<TaskEntry> setEditorBinder(AbstractTaskFormLayout form) {
        GridInlineEditorTaskNodeFormLayout<TaskEntry> teForm = (GridInlineEditorTaskNodeFormLayout<TaskEntry>) form;
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

        treeGrid.getDataProvider().addDataProviderListener(event -> {
            treeGrid.expand(settings.expandedEntries());
        });
    }

    protected Collection<Component> configureAdditionalTopBarComponents() {
        List<Component> components = new LinkedList<>();
        if (visibleColumns.containsKey(ColumnKey.FOCUS) && visibleColumns.get(ColumnKey.FOCUS)) {
            this.nestedTabs = new NestedTaskTabs(this);

            DoubleClickListenerUtil.add(treeGrid, entry ->
                    nestedTabs.onSelectNewRootEntry(entry));

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
            Icon check = new Icon(VaadinIcon.CHECK);
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
            cronHeaderLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            editHeaderLayout.add(cronHeaderLayout);
        }

        if (editHeaderLayout.getElement().getChildCount() > 0) {
            Span label = new Span("Edit selected:");
            editHeaderLayout.addComponentAsFirst(label);
            treeGrid.addSelectionListener(e -> label.setText("Edit selected: " + treeGrid.getSelectedItems().size()
                    + " items"));

            editHeaderLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 3),
                    new FormLayout.ResponsiveStep(K.SHORT_WIDTH, 6));

            return editHeaderLayout;
        }

        return editHeaderLayout;
    }

    @Override
    public Optional<TaskNode> rootNode() {
        return gridDataProvider.rootEntry() != null
                ? Optional.of(gridDataProvider.rootEntry().node())
                : Optional.empty();
    }

    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {

        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
            super(grid);

            GridMenuItem<TaskEntry> activeTask = addItem("");
            GridSubMenu<TaskEntry> activeTaskSubMenu = activeTask.getSubMenu();

            GridMenuItem<TaskEntry> startRoutine = addItem("Start Routine", e -> e.getItem().ifPresent(
                    entry -> {
                        controller.createRoutine(entry.node());
                        UI.getCurrent().navigate(RoutineView.class);
                    }
            ));

            GridMenuItem<TaskEntry> startRoutineWithSpecificDuration = addItem("Start Routine For Duration...", e -> e.getItem().ifPresent(
                    entry -> {
                        controller.createRoutine(entry.node());
                        UI.getCurrent().navigate(RoutineView.class);
                    }
            ));

            GridMenuItem<TaskEntry> resetSchedule = addItem("Reset Schedule", e -> e.getItem().ifPresent(
                    entry -> controller.requestChangeAsync(new OverrideScheduledForChange(
                            entry.node().linkId(), LocalDateTime.now()))));

            BiConsumer<String, InsertLocation> addInsertItem = (label, location) -> {
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
            };

            addInsertItem.accept("Before", InsertLocation.BEFORE);
            addInsertItem.accept("After", InsertLocation.AFTER);
            addInsertItem.accept("As First Subtask", InsertLocation.FIRST);
            addInsertItem.accept("As Last Subtask", InsertLocation.LAST);

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

            BiConsumer<String, InsertLocation> addMoveSelectedItem = (label, location) -> {
                int reverse = (location == InsertLocation.AFTER || location == InsertLocation.FIRST) ? -1 : 1;
                moveSelectedSubMenu.addItem(label, e -> {
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
                            .map(entry -> (Change) new MoveChange(
                                    entry.node().linkId(),
                                    e.getItem().get().node().id(),
                                    location))
                            .toList());

                    grid.getSelectionModel().deselectAll();
                });
            };

            addMoveSelectedItem.accept("As Subtasks At Front", InsertLocation.FIRST);
            addMoveSelectedItem.accept("As Subtasks At Back",  InsertLocation.LAST);
            addMoveSelectedItem.accept("Before", InsertLocation.BEFORE);
            addMoveSelectedItem.accept("After", InsertLocation.AFTER);

            add(new Hr());

            GridMenuItem<TaskEntry> remove = addItem("Remove");

            GridMenuItem<TaskEntry> confirmRemove = remove.getSubMenu().addItem("Confirm?", e -> e.getItem().ifPresent(
                    entry -> controller.requestChangeAsync(new DeleteChange<>(entry.node().linkId()))));

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
                            }
                        } catch (Exception e) {
                            NotificationMessage.error(e);
                        }
                    } else {
                        activeTask.setVisible(false);
                    }

                    Set<TaskEntry> selected = grid.getSelectedItems();
                    int selectedSize = selected.size();
                    multiEdit.setVisible(selectedSize > 1);
                    if (selectedSize == 0) {
                        moveTarget.setVisible(true);
                        moveSelected.setVisible(false);
                        moveTarget.setText("Move " + entry.task().name());
                    } else {
                        moveTarget.setVisible(false);
                        moveSelected.setVisible(true);
                        moveSelected.setText("Move " + selectedSize + " tasks");
                        multiEdit.setText("Edit " + selectedSize + " tasks");
                    }

                    activeTaskSubMenu.getItems().forEach(menuItem -> menuItem.setEnabled(hasActiveTaskProvider));

                    startRoutine.setEnabled(true);
                    remove.setEnabled(true);
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
