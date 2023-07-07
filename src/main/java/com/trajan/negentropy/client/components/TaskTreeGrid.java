package com.trajan.negentropy.client.components;

import com.google.common.base.Joiner;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.TaskEntryFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskEntry;
import com.trajan.negentropy.client.routine.RoutineView;
import com.trajan.negentropy.client.session.DescriptionViewDefaultSetting;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.tree.components.InlineIconButton;
import com.trajan.negentropy.client.tree.components.NestedTaskTabs;
import com.trajan.negentropy.client.tree.components.RetainOpenedMenuItemDecorator;
import com.trajan.negentropy.client.util.CronValueProvider;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SpringComponent
@RouteScope // TODO: Route vs UI scope?
@Scope("prototype")
@Slf4j
@Accessors(fluent = true)
@Getter
public class TaskTreeGrid extends Div {
    @Autowired private ClientDataController controller;
    @Autowired private SessionSettings settings;

    @Autowired private CronValueProvider cronValueProvider;

    public static final String COLUMN_KEY_DRAG_HANDLE = "Drag Handle";
    public static final String COLUMN_ID_DRAG_HANDLE = "drag-handle-column";
    public static final String COLUMN_KEY_NAME = "Name";
    public static final String COLUMN_KEY_FOCUS = "Focus";
    public static final String COLUMN_KEY_BLOCK = "Block";
    public static final String COLUMN_KEY_COMPLETE = "Complete";
    public static final String COLUMN_KEY_RECURRING = "Recurring";
    public static final String COLUMN_KEY_CRON = "Cron";
    public static final String COLUMN_KEY_SCHEDULED_FOR = "Scheduled For";
    public static final String COLUMN_KEY_TAGS = "Tags";
    public static final String COLUMN_KEY_DESCRIPTION = "Description";
    public static final String COLUMN_KEY_DURATION = "Single Step Duration";
    public static final String COLUMN_KEY_TIME_ESTIMATE = "Total Duration";
    public static final String COLUMN_KEY_EDIT = "Edit";
    public static final String COLUMN_KEY_DELETE = "Delete";

    // All columns but name
    public static final List<String> VISIBILITY_TOGGLEABLE_COLUMNS = List.of(
            COLUMN_KEY_DRAG_HANDLE,
            COLUMN_KEY_FOCUS,
            COLUMN_KEY_BLOCK,
            COLUMN_KEY_COMPLETE,
            COLUMN_KEY_RECURRING,
            COLUMN_KEY_CRON,
            COLUMN_KEY_SCHEDULED_FOR,
            COLUMN_KEY_TAGS,
            COLUMN_KEY_DESCRIPTION,
            COLUMN_KEY_DURATION,
            COLUMN_KEY_TIME_ESTIMATE,
            COLUMN_KEY_EDIT,
            COLUMN_KEY_DELETE
    );

    private TreeGrid<TaskEntry> treeGrid;
    private NestedTaskTabs nestedTabs;
    private HorizontalLayout topBar;

    private Grid.Column<TaskEntry> dragHandleColumn;
    private Grid.Column<TaskEntry> focusColumn;
    private Grid.Column<TaskEntry> nameColumn;
    private Grid.Column<TaskEntry> blockColumn;
    private Grid.Column<TaskEntry> completeColumn;
    private Grid.Column<TaskEntry> recurringColumn;
    private Grid.Column<TaskEntry> cron_column;
    private Grid.Column<TaskEntry> scheduled_for_column;
    private Grid.Column<TaskEntry> tagColumn;
    private Grid.Column<TaskEntry> descriptionColumn;
    private Grid.Column<TaskEntry> taskDurationColumn;
    private Grid.Column<TaskEntry> timeEstimateColumn;
    private Grid.Column<TaskEntry> editColumn;
    private Grid.Column<TaskEntry> deleteColumn;

    private TaskEntry draggedItem;

    private static final String DURATION_COL_WIDTH = "60px";
    private static final String ICON_COL_WIDTH_S = "31px";
    private static final String ICON_COL_WIDTH_L = "35px";

    private Editor<TaskEntry> editor;

    private List<String> columns;
    private Map<String, Boolean> visibleColumns;

    public void init(Map<String, Boolean> visibleColumns) {
        this.treeGrid = new TreeGrid<>(TaskEntry.class);
        this.columns = new ArrayList<>(VISIBILITY_TOGGLEABLE_COLUMNS);
        this.visibleColumns = visibleColumns;

        this.editor = treeGrid.getEditor();

        if (settings.enableContextMenu()) {
            new TaskTreeContextMenu(treeGrid);
        }

        topBar = new HorizontalLayout();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setWidthFull();

        this.add(
                topBar,
                treeGrid);

        this.initReadColumns();
        this.initEditColumns();
        this.initDetails();
        this.configureDragAndDrop();
        this.configureAdditionalEvents();
        this.configureSelectionMode();
        this.configureNestedTaskTabs();

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        treeGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        topBar.add(gridOptionsMenu());
        this.add(topBar, treeGrid);
    }

    private Icon headerIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.setSize(K.INLINE_ICON_SIZE);
        icon.addClassName(K.ICON_COLOR_GRAYED);
        return icon;
    }

    private String inlineLineAwesomeIconLitExpression(LineAwesomeIcon lineAwesomeIcon, String attributes) {
        return "<span class=\"grid-icon-lineawesome\" " + attributes + " style=\"-webkit-mask-position: var(--mask-position); display: inline-block; -webkit-mask-repeat: var(--mask-repeat); vertical-align: middle; --mask-repeat: no-repeat; background-color: currentcolor; --_size: var(--lumo-icon-size-m); flex: 0 0 auto; width: var(--_size); --mask-position: 50%; -webkit-mask-image: var(--mask-image); " +
                "--mask-image: url('line-awesome/svg/" + lineAwesomeIcon.getSvgName() + ".svg'); height: var(--_size);\"></span>";
    }

    private String inlineVaadinIconLitExpression(String iconName, String attributes) {
        return "<vaadin-icon class=\"grid-icon-vaadin\" icon=\"vaadin:" + iconName + "\" " +
                "@click=\"${onClick}\" " +
                attributes + "></vaadin-icon>";
    }

    private void initReadColumns() {
        if (columns.contains(COLUMN_KEY_DRAG_HANDLE)) {
            SerializableConsumer<TaskEntry> onDown = entry -> {
                treeGrid.setRowsDraggable(true);
                draggedItem = entry;
            };

            dragHandleColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineLineAwesomeIconLitExpression(LineAwesomeIcon.GRIP_LINES_VERTICAL_SOLID,
                                            "@mousedown=\"${onDown}\" @touchstart=\"${onDown}\" "))
                            .withFunction("onDown", onDown)
                    )
                    .setKey(COLUMN_KEY_DRAG_HANDLE)
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFrozen(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
            dragHandleColumn
                    .setId(COLUMN_ID_DRAG_HANDLE);
        }

        // Mandatory column
        nameColumn = treeGrid.addHierarchyColumn(
                        entry -> entry.task().name())
                .setKey(COLUMN_KEY_NAME)
                .setHeader(COLUMN_KEY_NAME)
                .setWidth("150px")
                .setFrozen(true)
                .setFlexGrow(1);

        if (columns.contains(COLUMN_KEY_FOCUS)) {
            focusColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("expand-full",
                                            "expand "))
                            .withFunction("onClick", entry ->
                                    nestedTabs.onSelectNewRootEntry(entry))
                    )
                    .setKey(COLUMN_KEY_FOCUS)
                    .setHeader(headerIcon(VaadinIcon.EXPAND_SQUARE))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_BLOCK)) {
            blockColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("bookmark-o",
                                            "?active=\"${item.block}\" "))
                            .withFunction("onClick", entry ->
                                    controller.updateTask(entry.task()
                                            .block(!entry.task().block())))
                            .withProperty("block", entry ->
                                    entry.task().block())
                    )
                    .setKey(COLUMN_KEY_BLOCK)
                    .setHeader(headerIcon(VaadinIcon.BOOKMARK))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_COMPLETE)) {
            completeColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("check",
                                            "?active=\"${!item.completed}\" "))
                            .withFunction("onClick", entry ->
                                    controller.updateNode(entry.node()
                                            .completed(!entry.node().completed())))
                            .withProperty("completed", entry ->
                                    entry.node().completed())
                    )
                    .setKey(COLUMN_KEY_COMPLETE)
                    .setHeader(headerIcon(VaadinIcon.CHECK_SQUARE_O))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_RECURRING)) {
            recurringColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("time-forward",
                                            "?active=\"${item.recurring}\" "))
                            .withFunction("onClick", entry ->
                                    controller.updateNode(entry.node()
                                            .recurring(!entry.node().recurring())))
                            .withProperty("recurring", entry ->
                                    entry.node().recurring())
                    )
                    .setKey(COLUMN_KEY_RECURRING)
                    .setHeader(headerIcon(VaadinIcon.TIME_FORWARD))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_CRON)) {
            cron_column = treeGrid.addColumn(
                            entry -> cronValueProvider.apply(entry.node().cron()))
                    .setKey(COLUMN_KEY_CRON)
                    .setHeader(headerIcon(VaadinIcon.CALENDAR_CLOCK))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_SCHEDULED_FOR)) {
            scheduled_for_column = treeGrid.addColumn(entry -> {
                        if (entry.node().cron() != null) {
                            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
                            return formatter.format(entry.node().scheduledFor());
                        } else {
                            return "-- -- -- -- -- -- --";
                        }
                    })
                    .setKey(COLUMN_KEY_SCHEDULED_FOR)
                    .setHeader(headerIcon(VaadinIcon.CALENDAR))
                    .setAutoWidth(true)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_TAGS)) {
            tagColumn = treeGrid.addColumn(
                            new ComponentRenderer<>(entry -> {
                                TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
                                tagComboBox.setWidthFull();
                                tagComboBox.setClassName("grid-combo-box");
                                tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                                tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                                tagComboBox.setValue(entry.task().tags());
                                tagComboBox.addValueChangeListener(event -> {
                                    Task task = new Task(entry.task().id())
                                            .tags(event.getValue());
                                    controller.updateTask(task);
                                });
                                return tagComboBox;
                            }))
                    .setKey(COLUMN_KEY_TAGS)
                    .setHeader(COLUMN_KEY_TAGS)
                    .setFlexGrow(1);
            tagColumn.setClassName("tag-column");
        }

        if (columns.contains(COLUMN_KEY_DESCRIPTION)) {
            descriptionColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("eye",
                                                    "?active=\"${item.hasDescription}\""))
                            .withFunction("onClick", entry ->
                                    treeGrid.setDetailsVisible(
                                            entry,
                                            !treeGrid.isDetailsVisible(entry)))
                            .withProperty("hasDescription", entry ->
                                    !entry.task().description().isBlank()))
                    .setKey(COLUMN_KEY_DESCRIPTION)
                    .setHeader(headerIcon(VaadinIcon.CLIPBOARD_TEXT))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

//        Grid.Column<TaskEntry> priorityColumn = treeGrid
//                .addColumn(entry ->
//                        entry.link().getPriority())
//                .setHeader("Priority")
//                .setAutoWidth(true)
//                .setFlexGrow(0);

        if (columns.contains((COLUMN_KEY_DURATION))) {
            taskDurationColumn = treeGrid.addColumn(
                            new DurationEstimateValueProvider<>(
                                    controller.queryService(),
                                    () -> TimeFormat.DURATION,
                                    false))
                    .setKey(COLUMN_KEY_DURATION)
                    .setHeader(headerIcon(VaadinIcon.CLOCK))
                    .setWidth(DURATION_COL_WIDTH)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains((COLUMN_KEY_TIME_ESTIMATE))) {
            HorizontalLayout timeEstimateColumnHeader = new HorizontalLayout(
                    headerIcon(VaadinIcon.FILE_TREE_SUB),
                    headerIcon(VaadinIcon.CLOCK));
            timeEstimateColumnHeader.setSpacing(false);
            timeEstimateColumnHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            timeEstimateColumn = treeGrid.addColumn(
                            new DurationEstimateValueProvider<>(
                                    controller.queryService(),
                                    () -> TimeFormat.DURATION,
                                    true))
                    .setKey(COLUMN_KEY_TIME_ESTIMATE)
                    .setHeader(timeEstimateColumnHeader)
                    .setWidth(DURATION_COL_WIDTH)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_EDIT)) {
            editColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("edit",
                                            "active"))
                            .withFunction("onClick", entry -> {
                                if (editor.isOpen()) {
                                    editor.cancel();
                                }
                                editor.editItem(entry);
                            })
                    )
                    .setKey(COLUMN_KEY_EDIT)
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        if (columns.contains(COLUMN_KEY_DELETE)) {
            deleteColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
                                    inlineVaadinIconLitExpression("trash",
                                            "?hidden=\"${item.isHidden}\" " +
                                                    "delete"))
                            .withFunction("onClick", entry ->
                                    controller.deleteNode(entry))
                            .withProperty("isHidden", entry ->
                                    entry.task().hasChildren())
                    )
                    .setKey(COLUMN_KEY_DELETE)
                    .setHeader(headerIcon(VaadinIcon.TRASH))
                    .setWidth(ICON_COL_WIDTH_L)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER);
        }

        treeGrid.setSortableColumns();
        treeGrid.setColumnReorderingAllowed(true);

        treeGrid.setPartNameGenerator(entry -> {
            List<String> partNames = new ArrayList<>();

            if (entry.node().completed()) {
                partNames.add(K.GRID_PARTNAME_COMPLETED);
            }
            if (entry.task().block()) {
                partNames.add(K.GRID_PARTNAME_BLOCK);
            }

            return Joiner.on(" ").join(partNames);
        });
    }

    private TaskEntryFormLayout detailsForm(TaskEntry entry) {
        TaskEntryFormLayout form = new TaskEntryFormLayout(controller, entry);
        form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
                LumoUtility.BoxSizing.BORDER);
        form.onClear(() -> editor.cancel());
        form.onSave(() -> editor.save());

        editor.setBinder(form.binder());

        return form;
    }

    private HorizontalLayout detailsDescription(TaskEntry entry) {
        TextArea descriptionArea = new TextArea();
        descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
        descriptionArea.setWidthFull();

        InlineIconButton descriptionSaveButton = new InlineIconButton(VaadinIcon.CHECK.create());
        InlineIconButton descriptionCancelButton = new InlineIconButton(VaadinIcon.CLOSE.create());
        HorizontalLayout container = new HorizontalLayout(
                descriptionArea,
                descriptionSaveButton,
                descriptionCancelButton);

        descriptionArea.setValue(entry.task().description());
        descriptionArea.setReadOnly(true);
        descriptionSaveButton.setVisible(false);
        descriptionCancelButton.setVisible(false);

        Consumer<Boolean> toggleEditing = editing -> {
            descriptionArea.setReadOnly(!editing);
            descriptionSaveButton.setVisible(editing);
            descriptionCancelButton.setVisible(editing);
        };

        Runnable saveDescription = () -> {
            toggleEditing.accept(false);

            controller.updateTask(new Task(entry.task().id())
                    .description(descriptionArea.getValue()));
        };

        Runnable cancelEditingDescription = () -> {
            toggleEditing.accept(false);

            descriptionArea.setValue(entry.task().description());
        };

        descriptionArea.getElement().addEventListener("mouseup", e -> toggleEditing.accept(true));
        descriptionArea.getElement().addEventListener("touchend", e -> toggleEditing.accept(true));

        descriptionSaveButton.addClickListener(e -> saveDescription.run());
        descriptionCancelButton.addClickListener(e -> cancelEditingDescription.run());

        Registration enterShortcut = Shortcuts.addShortcutListener(container,
                saveDescription::run,
                Key.ENTER);

        Registration escapeShortcut = Shortcuts.addShortcutListener(container,
                cancelEditingDescription::run,
                Key.ESCAPE);

        return container;
    }

    private void initDetails() {
        ComponentRenderer<Component, TaskEntry> detailsRenderer = new ComponentRenderer<>(entry -> {
            Predicate<TaskEntry> isBeingEdited = ntry -> ntry.equals(editor.getItem());

            if (isBeingEdited.test(entry) && columns.contains(COLUMN_KEY_EDIT)) {
                return detailsForm(entry);
            } else {
                return detailsDescription(entry);
            }
        });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
        treeGrid.setDetailsVisibleOnClick(false);
    }

    private void initEditColumns() {
        if (columns.contains(COLUMN_KEY_EDIT)) {
            Editor<TaskEntry> editor = treeGrid.getEditor();
            editor.addSaveListener(e -> controller.updateEntry(e.getItem()));

            editor.setBuffered(true);

            InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
            check.addClickListener(e -> editor.save());

            AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
            AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());

            editor.addOpenListener(e -> {
                treeGrid.setDetailsVisible(e.getItem(), true);
                escapeListener.get().ifPresent(Registration::remove);
                enterListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
                        editor::save,
                        Key.ENTER)));
            });

            editor.addCloseListener(e -> {
                treeGrid.setDetailsVisible(e.getItem(), false);
                enterListener.get().ifPresent(Registration::remove);
                treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
                escapeListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
                        editor::cancel,
                        Key.ESCAPE)));
                if (e.getItem().task().description().isBlank()) {
                    treeGrid.setDetailsVisible(e.getItem(), false);
                }
            });

            editColumn.setEditorComponent(check);
        }
    }

    private void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(false);
        // Drag start is managed by the dragHandleColumn

        if (columns.contains(COLUMN_KEY_DRAG_HANDLE)) {
            treeGrid.addDropListener(event -> {
                log.debug(draggedItem + " dropped onto " +
                        event.getDropTargetItem().orElseThrow().task().name());
                if (event.getDropTargetItem().isPresent()) {
                    TaskEntry target = event.getDropTargetItem().get();
                    if (!draggedItem.equals(target)) {
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
                        NotificationError.show("Cannot move item onto itself");
                    }
                }
                draggedItem = null;
                treeGrid.setRowsDraggable(false);
            });
        }
    }

    private void configureAdditionalEvents() {
        treeGrid.addExpandListener(event ->
                settings.expandedEntries().addAll(event.getItems()));

        treeGrid.addCollapseListener(event ->
                settings.expandedEntries().removeAll(event.getItems()));

        // TODO: Needs an in-memory TreeDataProvider to actually work
        Consumer<TaskEntry> detailsVisibilitySwitch = entry -> {
            switch (settings.descriptionViewDefaultSetting()) {
                case ALL -> treeGrid.setDetailsVisible(entry, true);
                case IF_PRESENT -> treeGrid.setDetailsVisible(
                        entry, !entry.task().description().isBlank());
                case NONE -> treeGrid.setDetailsVisible(entry, false);
            }
        };

        treeGrid.addExpandListener(event -> {
            Collection<TaskEntry> entries = event.getItems();
            entries.forEach(detailsVisibilitySwitch);
        });

        treeGrid.getDataProvider().addDataProviderListener(event -> {
            treeGrid.expand(settings.expandedEntries());
        });
    }

    private void configureSelectionMode() {
        treeGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    }

    private void configureNestedTaskTabs() {
        if (columns.contains(COLUMN_KEY_FOCUS)) {
            this.nestedTabs = new NestedTaskTabs(controller);

            DoubleClickListenerUtil.add(treeGrid, entry ->
                    nestedTabs.onSelectNewRootEntry(entry));

            topBar.add(nestedTabs);
        }
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
            if (eventDetail.getString("columnId").equals(COLUMN_ID_DRAG_HANDLE)) {
                return false;
            }
            return super.onBeforeOpenMenu(eventDetail);
        }
    }

    private BiConsumer<String, Boolean> setColumnVisibility = (column, visible) ->
            visibleColumns.put(column, visible);

    private Function<String, Boolean> getColumnVisibility = column ->
            visibleColumns.get(column);

    private Div gridOptionsMenu() {
        MenuBar menuBar = new MenuBar();
        menuBar.setWidth(ICON_COL_WIDTH_S);
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        menuBar.getElement().setProperty("closeOn", "vaadin-overlay-outside-click");

        InlineIconButton menuIcon = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = menuBar.addItem(menuIcon)
                .getSubMenu();

        MenuItem visibilityMenu = subMenu.addItem("Column Visibility");

        TriConsumer<Grid.Column<TaskEntry>, MenuItem, Boolean> toggleVisibility = (column, menuItem, checked) -> {
            menuItem.setChecked(checked);
            this.setColumnVisibility.accept(column.getKey(), checked);
            column.setVisible(checked);
        };

        VISIBILITY_TOGGLEABLE_COLUMNS.forEach(
                (columnKey) -> {
                    Grid.Column<TaskEntry> column = treeGrid.getColumnByKey(columnKey);
                    MenuItem menuItem = visibilityMenu.getSubMenu().addItem(columnKey);
                    menuItem.setCheckable(true);

                    // TODO: Use visibleColumns
                    if (column != null) {
                        toggleVisibility.accept(column, menuItem, this.getColumnVisibility.apply(columnKey));
                        menuItem.addClickListener(e -> toggleVisibility.accept(
                                column, menuItem, menuItem.isChecked()));

                        RetainOpenedMenuItemDecorator.keepOpenOnClick(menuItem);
                    }


                }
        );

        MenuItem descriptionsView = subMenu.addItem("Descriptions");

        descriptionsView.getSubMenu().addItem("Expand Existing", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.IF_PRESENT));
        descriptionsView.getSubMenu().addItem("Expand All", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.ALL));
        descriptionsView.getSubMenu().addItem("Hide All", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.NONE));

        return new Div(menuBar);
    }
}
