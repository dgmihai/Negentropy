//package com.trajan.negentropy.client.tree;
//
//import com.trajan.negentropy.client.K;
//import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
//import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
//import com.trajan.negentropy.client.components.taskform.TaskEntryFormLayout;
//import com.trajan.negentropy.client.controller.ClientDataController;
//import com.trajan.negentropy.client.controller.data.TaskEntry;
//import com.trajan.negentropy.client.routine.RoutineView;
//import com.trajan.negentropy.client.session.DescriptionViewDefaultSetting;
//import com.trajan.negentropy.client.session.SessionSettings;
//import com.trajan.negentropy.client.tree.components.InlineIconButton;
//import com.trajan.negentropy.client.tree.components.InlineIconToggleButton;
//import com.trajan.negentropy.client.tree.components.NestedTaskTabs;
//import com.trajan.negentropy.client.tree.components.RetainOpenedMenuItemDecorator;
//import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
//import com.trajan.negentropy.client.util.NotificationError;
//import com.trajan.negentropy.client.util.TimeFormat;
//import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
//import com.trajan.negentropy.server.facade.model.Task;
//import com.trajan.negentropy.util.ExecTimer;
//import com.vaadin.flow.component.Component;
//import com.vaadin.flow.component.Key;
//import com.vaadin.flow.component.Shortcuts;
//import com.vaadin.flow.component.UI;
//import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
//import com.vaadin.flow.component.contextmenu.MenuItem;
//import com.vaadin.flow.component.contextmenu.SubMenu;
//import com.vaadin.flow.component.grid.ColumnTextAlign;
//import com.vaadin.flow.component.grid.Grid;
//import com.vaadin.flow.component.grid.GridVariant;
//import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
//import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
//import com.vaadin.flow.component.grid.contextmenu.GridSubMenu;
//import com.vaadin.flow.component.grid.dnd.GridDropMode;
//import com.vaadin.flow.component.grid.editor.Editor;
//import com.vaadin.flow.component.html.Div;
//import com.vaadin.flow.component.html.Hr;
//import com.vaadin.flow.component.icon.Icon;
//import com.vaadin.flow.component.icon.VaadinIcon;
//import com.vaadin.flow.component.menubar.MenuBar;
//import com.vaadin.flow.component.menubar.MenuBarVariant;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.tabs.TabsVariant;
//import com.vaadin.flow.component.textfield.TextArea;
//import com.vaadin.flow.component.textfield.TextAreaVariant;
//import com.vaadin.flow.component.treegrid.TreeGrid;
//import com.vaadin.flow.data.renderer.ComponentRenderer;
//import com.vaadin.flow.data.renderer.LitRenderer;
//import com.vaadin.flow.data.value.ValueChangeMode;
//import com.vaadin.flow.function.SerializableConsumer;
//import com.vaadin.flow.shared.Registration;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import com.vaadin.flow.spring.annotation.UIScope;
//import com.vaadin.flow.theme.lumo.LumoUtility;
//import elemental.json.JsonObject;
//import jakarta.annotation.PostConstruct;
//import lombok.Getter;
//import lombok.experimental.Accessors;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.logging.log4j.util.TriConsumer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.vaadin.lineawesome.LineAwesomeIcon;
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
//@SpringComponent
//@UIScope
//@Slf4j
//@Accessors(fluent = true)
//@Getter
//public class TaskTreeGrid extends VerticalLayout {
//    @Autowired private ExecTimer execTimer;
//
//    @Autowired private ClientDataController controller;
//    @Autowired private SessionSettings settings;
//
//    private TreeGrid<TaskEntry> treeGrid;
//    private NestedTaskTabs nestedTabs;
//
//    private Grid.Column<TaskEntry> dragHandleColumn;
//    private Grid.Column<TaskEntry> focusColumn;
//    private Grid.Column<TaskEntry> nameColumn;
//    private Grid.Column<TaskEntry> completeColumn;
//    private Grid.Column<TaskEntry> recurringColumn;
//    private Grid.Column<TaskEntry> tagColumn;
//    private Grid.Column<TaskEntry> descriptionColumn;
//    private Grid.Column<TaskEntry> taskDurationColumn;
//    private Grid.Column<TaskEntry> timeEstimateColumn;
//    private Grid.Column<TaskEntry> editColumn;
//    private Grid.Column<TaskEntry> deleteColumn;
//
//    private TaskEntry draggedItem;
//
//    private final String DURATION_COL_WIDTH = "60px";
//    private final String ICON_COL_WIDTH_S = "31px";
//    private final String ICON_COL_WIDTH_L = "35px";
//
//    private Editor<TaskEntry> editor;
//
//    public static final String COLUMN_KEY_DRAG_HANDLE = "Drag Handle";
//    public static final String COLUMN_ID_DRAG_HANDLE = "drag-handle-column";
//    public static final String COLUMN_KEY_NAME = "Name";
//    public static final String COLUMN_KEY_FOCUS = "Focus";
//    public static final String COLUMN_KEY_COMPLETE = "Complete";
//    public static final String COLUMN_KEY_RECURRING = "Recurring";
//    public static final String COLUMN_KEY_TAGS = "Tags";
//    public static final String COLUMN_KEY_DESCRIPTION = "Description";
//    public static final String COLUMN_KEY_DURATION = "Single Step Duration";
//    public static final String COLUMN_KEY_TIME_ESTIMATE = "Total Duration";
//    public static final String COLUMN_KEY_EDIT = "Edit";
//    public static final String COLUMN_KEY_DELETE = "Delete";
//    public static final List<String> VISIBILITY_TOGGLEABLE_COLUMNS = List.of(
//            COLUMN_KEY_DRAG_HANDLE,
//            COLUMN_KEY_FOCUS,
//            COLUMN_KEY_COMPLETE,
//            COLUMN_KEY_RECURRING,
//            COLUMN_KEY_TAGS,
//            COLUMN_KEY_DESCRIPTION,
//            COLUMN_KEY_DURATION,
//            COLUMN_KEY_TIME_ESTIMATE,
//            COLUMN_KEY_EDIT,
//            COLUMN_KEY_DELETE
//    );
//
//    @PostConstruct
//    public void init() {
//        execTimer.mark("TreeGrid init");
//
//        this.treeGrid = new TreeGrid<>(TaskEntry.class);
//        treeGrid.setDataProvider(controller.dataProvider());
//
//        execTimer.mark("Nested Tabs");
//        this.nestedTabs = new NestedTaskTabs(controller);
//        new TaskTreeContextMenu(treeGrid);
//        this.editor = treeGrid.getEditor();
//
//        execTimer.mark("initReadColumns");
//        this.initReadColumns();
//
//        execTimer.mark("initEditColumns");
//        this.initEditColumns();
//
//        execTimer.mark("initDetails");
//        this.initDetails();
//
//        execTimer.mark("configureDetailsVisibility");
//        this.configureDetailsVisibility();
//
//        execTimer.mark("configureDragAndDrop");
//        this.configureDragAndDrop();
//
//        execTimer.mark("configureSelection");
//        this.configureAdditionalEvents();
//
//        execTimer.mark("TaskTreeGrid layout");
//        nestedTabs.addThemeVariants(TabsVariant.LUMO_SMALL);
//
//        treeGrid.setHeightFull();
//        treeGrid.setWidthFull();
//        // TODO: Wrap cell content?
//        treeGrid.addThemeVariants(
//                GridVariant.LUMO_COMPACT,
//                GridVariant.LUMO_WRAP_CELL_CONTENT);
//        this.setPadding(false);
//
//        HorizontalLayout toolbar = new HorizontalLayout(
//                nestedTabs,
//                gridOptionsMenu());
//        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
//        toolbar.setWidthFull();
//
//        this.add(
//                toolbar,
//                treeGrid);
//        execTimer.mark("TaskTreeGrid finished");
//    }
//
//    private Icon headerIcon(VaadinIcon vaadinIcon) {
//            Icon icon = vaadinIcon.create();
//            icon.setSize(K.INLINE_ICON_SIZE);
//            icon.addClassName(K.ICON_COLOR_GRAYED);
//            return icon;
//    }
//
//    private String inlineLineAwesomeIconLitExpression(LineAwesomeIcon lineAwesomeIcon, String attributes) {
//        return "<span class=\"grid-icon-lineawesome\" " + attributes + " style=\"-webkit-mask-position: var(--mask-position); display: inline-block; -webkit-mask-repeat: var(--mask-repeat); vertical-align: middle; --mask-repeat: no-repeat; background-color: currentcolor; --_size: var(--lumo-icon-size-m); flex: 0 0 auto; width: var(--_size); --mask-position: 50%; -webkit-mask-image: var(--mask-image); " +
//                "--mask-image: url('line-awesome/svg/" + lineAwesomeIcon.getSvgName() + ".svg'); height: var(--_size);\"></span>";
//    }
//
//    private String inlineVaadinIconLitExpression(String iconName, String attributes) {
//        return "<vaadin-icon class=\"grid-icon-vaadin\" icon=\"vaadin:" + iconName + "\" " +
//                "@click=\"${onClick}\" " +
//                attributes + "></vaadin-icon>";
//    }
//
//    private void initReadColumns() {
//        SerializableConsumer<TaskEntry> onDown = entry -> {
//            treeGrid.setRowsDraggable(true);
//            draggedItem = entry;
//        };
//
//        dragHandleColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                inlineLineAwesomeIconLitExpression(LineAwesomeIcon.GRIP_LINES_VERTICAL_SOLID,
//                        "@mousedown=\"${onDown}\" @touchstart=\"${onDown}\" "))
//                        .withFunction("onDown", onDown)
//                )
//                .setKey(COLUMN_KEY_DRAG_HANDLE)
//                .setWidth(ICON_COL_WIDTH_L)
//                .setFrozen(true)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//        dragHandleColumn
//                .setId(COLUMN_ID_DRAG_HANDLE);
//
//        nameColumn = treeGrid.addHierarchyColumn(
//                        entry -> entry.task().name())
//                .setKey(COLUMN_KEY_NAME)
//                .setHeader(COLUMN_KEY_NAME)
//                .setWidth("150px")
//                .setFrozen(true)
//                .setFlexGrow(1);
//
//        focusColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                                inlineVaadinIconLitExpression("expand-full",
//                                        "expand "))
//                        .withFunction("onClick", entry ->
//                                nestedTabs.onSelectNewRootEntry(entry))
//                )
//                .setKey(COLUMN_KEY_FOCUS)
//                .setHeader(headerIcon(VaadinIcon.EXPAND_SQUARE))
//                .setAutoWidth(true)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        completeColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                inlineVaadinIconLitExpression("check",
//                        "?active=\"${!item.recurring}\" " +
//                                "?hidden=\"${item.hasChildren}\""))
//                        .withFunction("onClick", entry ->
//                                controller.deleteNode(entry))
//                        .withProperty("recurring", entry ->
//                                entry.node().recurring())
//                        .withProperty("hasChildren", entry ->
//                                entry.task().hasChildren())
//                )
//                .setKey(COLUMN_KEY_COMPLETE)
//                .setHeader(headerIcon(VaadinIcon.CHECK_SQUARE_O))
//                .setWidth(ICON_COL_WIDTH_L)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        recurringColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                inlineVaadinIconLitExpression("time-forward",
//                        "?active=\"${item.recurring}\" "))
//                        .withFunction("onClick", entry ->
//                                entry.node().recurring(!entry.node().recurring()))
//                        .withProperty("recurring", entry ->
//                                entry.node().recurring())
//                )
//                .setKey(COLUMN_KEY_RECURRING)
//                .setHeader(headerIcon(VaadinIcon.TIME_FORWARD))
//                .setAutoWidth(true)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        tagColumn = treeGrid.addColumn(
//                new ComponentRenderer<>(entry -> {
//                    TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
//                    tagComboBox.setWidthFull();
//                    tagComboBox.setClassName("grid-combo-box");
//                    tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
//                    tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
//                    tagComboBox.setValue(entry.task().tags());
//                    tagComboBox.addValueChangeListener(event -> {
//                        Task task = new Task(entry.task().id())
//                                .tags(event.getValue());
//                        controller.updateTask(task);
//                    });
//                    return tagComboBox;
//                }))
//                .setKey(COLUMN_KEY_TAGS)
//                .setHeader(COLUMN_KEY_TAGS)
//                .setFlexGrow(1);
//        tagColumn.setClassName("tag-column");
//
//        descriptionColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                inlineVaadinIconLitExpression("eye",
//                        "?hidden=\"${!item.isHidden}\" " +
//                                "?active=\"${item.detailsVisible}\""))
//                        .withFunction("onClick", entry ->
//                                treeGrid.setDetailsVisible(
//                                        entry,
//                                        !treeGrid.isDetailsVisible(entry)))
//                        .withProperty("isHidden", entry ->
//                                !treeGrid.isDetailsVisible(entry)
//                                && !entry.task().description().isBlank())
//                        .withProperty("detailsVisible",treeGrid::isDetailsVisible)
//                )
//                .setKey(COLUMN_KEY_DESCRIPTION)
//                .setHeader(headerIcon(VaadinIcon.CLIPBOARD_TEXT))
//                .setWidth(ICON_COL_WIDTH_L)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
////        Grid.Column<TaskEntry> priorityColumn = treeGrid
////                .addColumn(entry ->
////                        entry.link().getPriority())
////                .setHeader("Priority")
////                .setAutoWidth(true)
////                .setFlexGrow(0);
//
//        taskDurationColumn = treeGrid.addColumn(
//                        new DurationEstimateValueProvider<>(
//                                controller.queryService(),
//                                () -> TimeFormat.DURATION,
//                                false))
//                .setKey(COLUMN_KEY_DURATION)
//                .setHeader(headerIcon(VaadinIcon.CLOCK))
//                .setWidth(DURATION_COL_WIDTH)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        HorizontalLayout timeEstimateColumnHeader = new HorizontalLayout(
//                headerIcon(VaadinIcon.FILE_TREE_SUB),
//                headerIcon(VaadinIcon.CLOCK));
//        timeEstimateColumnHeader.setSpacing(false);
//        timeEstimateColumnHeader.setJustifyContentMode(JustifyContentMode.CENTER);
////        timeEstimateColumnHeader.setSizeFull();
//        timeEstimateColumn = treeGrid.addColumn(
//                        new DurationEstimateValueProvider<>(
//                                controller.queryService(),
//                                () -> TimeFormat.DURATION,
//                                true))
//                .setKey(COLUMN_KEY_TIME_ESTIMATE)
//                .setHeader(timeEstimateColumnHeader)
//                .setWidth(DURATION_COL_WIDTH)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        editColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                inlineVaadinIconLitExpression("edit",
//                        "active"))
//                        .withFunction("onClick", entry -> {
//                            if (editor.isOpen()) {
//                                editor.cancel();
//                            }
//                            editor.editItem(entry);
//                        })
//                )
//                .setKey(COLUMN_KEY_EDIT)
//                .setWidth(ICON_COL_WIDTH_L)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        List<InlineIconButton> deleteIcons = new ArrayList<>();
//        InlineIconToggleButton trashIconButton = new InlineIconToggleButton(
//                VaadinIcon.EYE_SLASH.create(),
//                VaadinIcon.TRASH.create());
//        trashIconButton.onToggle(
//                () -> {
//                    deleteIcons.forEach(icon -> icon.setVisible(trashIconButton.activated()));
//                });
//        trashIconButton.activatedIcon().addClassName(K.ICON_COLOR_ERROR);
//        trashIconButton.deactivatedIcon().addClassName(K.ICON_COLOR_ERROR);
//        deleteColumn = treeGrid.addColumn(LitRenderer.<TaskEntry>of(
//                        inlineVaadinIconLitExpression("trash",
//                                "?hidden=\"${item.isHidden}\" " +
//                                        "delete"))
//                        .withFunction("onClick", entry ->
//                                controller.deleteNode(entry))
//                        .withProperty("isHidden", entry ->
//                                entry.task().hasChildren() && !trashIconButton.activated())
//                )
//                .setKey(COLUMN_KEY_DELETE)
//                .setHeader(trashIconButton)
//                .setWidth(ICON_COL_WIDTH_L)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);
//
//        treeGrid.setSortableColumns();
//        treeGrid.setColumnReorderingAllowed(true);
//
////        treeGrid.setPartNameGenerator(entry -> {
////            if (entry.node().recurring()) {
////                return K.GRID_PARTNAME_RECURRING;
////            } else {
////                return K.GRID_PARTNAME_NON_RECURRING;
////            }
////        });
//    }
//
//    private void initDetails() {
//        ComponentRenderer<Component, TaskEntry> detailsRenderer = new ComponentRenderer<>(entry -> {
//            Predicate<TaskEntry> isBeingEdited = ntry -> ntry.equals(editor.getItem());
//
//            if (isBeingEdited.test(entry)) {
//                TaskEntryFormLayout form = new TaskEntryFormLayout(controller, entry);
//                form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
//                        LumoUtility.BoxSizing.BORDER);
//                form.onClear(() -> editor.cancel());
//                form.onSave(() -> editor.save());
//
//                editor.setBinder(form.binder());
//
//                return form;
//            } else {
//                TextArea descriptionArea = new TextArea();
//                descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);
//                descriptionArea.addThemeVariants(TextAreaVariant.LUMO_SMALL);
//                descriptionArea.setWidthFull();
//
//                InlineIconButton descriptionSaveButton = new InlineIconButton(VaadinIcon.CHECK.create());
//                InlineIconButton descriptionCancelButton = new InlineIconButton(VaadinIcon.CLOSE.create());
//                HorizontalLayout container = new HorizontalLayout(
//                        descriptionArea,
//                        descriptionSaveButton,
//                        descriptionCancelButton);
//
//                descriptionArea.setValue(entry.task().description());
//                descriptionArea.setReadOnly(true);
//                descriptionSaveButton.setVisible(false);
//                descriptionCancelButton.setVisible(false);
//
//                Consumer<Boolean> toggleEditing = editing -> {
//                    descriptionArea.setReadOnly(!editing);
//                    descriptionSaveButton.setVisible(editing);
//                    descriptionCancelButton.setVisible(editing);
//                };
//
//                Runnable saveDescription = () -> {
//                    toggleEditing.accept(false);
//
//                    controller.updateTask(new Task(entry.task().id())
//                            .description(descriptionArea.getValue()));
//                };
//
//                Runnable cancelEditingDescription = () -> {
//                    toggleEditing.accept(false);
//
//                    descriptionArea.setValue(entry.task().description());
//                };
//
//                descriptionArea.getElement().addEventListener("mouseup", e -> toggleEditing.accept(true));
//                descriptionArea.getElement().addEventListener("touchend", e -> toggleEditing.accept(true));
//
//                descriptionSaveButton.addClickListener(e -> saveDescription.run());
//                descriptionCancelButton.addClickListener(e -> cancelEditingDescription.run());
//
//                Registration enterShortcut = Shortcuts.addShortcutListener(container,
//                        saveDescription::run,
//                        Key.ENTER);
//
//                Registration escapeShortcut = Shortcuts.addShortcutListener(container,
//                        cancelEditingDescription::run,
//                        Key.ESCAPE);
//
//                return container;
//            }
//        });
//
//        treeGrid.setItemDetailsRenderer(detailsRenderer);
//    }
//
//    private void configureDetailsVisibility() {
//
//    }
//
//    private void initEditColumns() {
//        Editor<TaskEntry> editor = treeGrid.getEditor();
//        editor.addSaveListener(e -> controller.updateTask(e.getItem()));
//
//        editor.setBuffered(true);
//
//        InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
//        check.addClickListener(e -> editor.save());
//
//        AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
//        AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());
//
//        editor.addOpenListener(e -> {
//            treeGrid.setDetailsVisible(e.getItem(), true);
//            escapeListener.get().ifPresent(Registration::remove);
//            enterListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
//                    editor::save,
//                    Key.ENTER)));
//        });
//
//        editor.addCloseListener(e -> {
//            treeGrid.setDetailsVisible(e.getItem(), false);
//            enterListener.get().ifPresent(Registration::remove);
//            treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
//            escapeListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
//                    editor::cancel,
//                    Key.ESCAPE)));
//            if (e.getItem().task().description().isBlank()) {
//                treeGrid.setDetailsVisible(e.getItem(), false);
//            }
//        });
//
//        editColumn.setEditorComponent(check);
//    }
//
//    private void configureDragAndDrop() {
//        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
//        treeGrid.setRowsDraggable(false);
//        // Drag start is managed by the dragHandleColumn
//
//        treeGrid.addDropListener(event -> {
//            log.debug(draggedItem + " dropped onto " +
//                    event.getDropTargetItem().orElseThrow().task().name());
//            if (event.getDropTargetItem().isPresent()) {
//                TaskEntry target = event.getDropTargetItem().get();
//                if (!draggedItem.equals(target)) {
//                    switch (event.getDropLocation()) {
//                        case ABOVE -> {
//                            controller.moveNodeBefore(
//                                    draggedItem,
//                                    target);
//                        }
//                        case BELOW -> {
//                            controller.moveNodeAfter(
//                                    draggedItem,
//                                    target);
//                        }
//                        case ON_TOP -> controller.moveNodeInto(
//                                draggedItem,
//                                target);
//                    }
//                } else {
//                    NotificationError.show("Cannot move item onto itself");
//                }
//            }
//            draggedItem = null;
//            treeGrid.setRowsDraggable(false);
//        });
//    }
//
//    private void configureAdditionalEvents() {
//        treeGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
//
//        DoubleClickListenerUtil.add(treeGrid, entry ->
//                nestedTabs.onSelectNewRootEntry(entry));
//
//        treeGrid.addExpandListener(event ->
//                settings.expandedEntries().addAll(event.getItems()));
//
//        treeGrid.addCollapseListener(event ->
//                settings.expandedEntries().removeAll(event.getItems()));
//
//        Consumer<TaskEntry> detailsVisibilitySwitch = entry -> {
//            switch (settings.descriptionViewDefaultSetting()) {
//                case ALL -> treeGrid.setDetailsVisible(entry, true);
//                case IF_PRESENT -> treeGrid.setDetailsVisible(
//                        entry, !entry.task().description().isBlank());
//                case NONE -> treeGrid.setDetailsVisible(entry, false);
//            }
//        };
//
//        treeGrid.addExpandListener(event -> {
//            Collection<TaskEntry> entries = event.getItems();
//            entries.forEach(detailsVisibilitySwitch);
//        });
//
//        treeGrid.getDataProvider().addDataProviderListener(event -> {
//            treeGrid.expand(settings.expandedEntries());
//        });
//    }
//
//    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {
//
//        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
//            super(grid);
//
//            GridMenuItem<TaskEntry> activeTask = addItem("");
//            GridSubMenu<TaskEntry> activeTaskSubMenu = activeTask.getSubMenu();
//
////            activeTask.setEnabled(false);
////            activeTask.addClassName(K.COLOR_PRIMARY);
//
//            GridMenuItem<TaskEntry> startRoutine = addItem("Start Routine", e -> e.getItem().ifPresent(
//                    entry -> {
//                        controller.createRoutine(entry.task().id());
//                        UI.getCurrent().navigate(RoutineView.class);
//                    }
//            ));
//
//            GridMenuItem<TaskEntry> insertBefore = activeTaskSubMenu.addItem("Before", e -> e.getItem().ifPresent(
//                    controller::addTaskFromActiveProviderBefore
//            ));
//
//            GridMenuItem<TaskEntry> insertAfter = activeTaskSubMenu.addItem("After", e -> e.getItem().ifPresent(
//                    controller::addTaskFromActiveProviderAfter
//            ));
//
//            GridMenuItem<TaskEntry> insertAsSubtask = activeTaskSubMenu.addItem("As subtask", e -> e.getItem().ifPresent(
//                    controller::addTaskFromActiveProviderAsChild
//            ));
//
//            add(new Hr());
//
//            addItem("Remove", e -> e.getItem().ifPresent(
//                    controller::deleteNode
//            ));
//
//            // Do not show context menu when header is clicked
//            setDynamicContentHandler(entry -> {
//                if (entry == null) {
//                    return false;
//                } else {
//                    boolean hasActiveTaskProvider =
//                            controller.activeTaskProvider() != null &&
//                            controller.activeTaskProvider().hasValidTask().success();
//
//                    if (hasActiveTaskProvider) {
//                        try {
//                            controller.activeTaskProvider().getTask().ifPresentOrElse(
//                                    task -> activeTask.setText("Insert " + task.name()), () -> {
//                                    });
//                            activeTask.setVisible(true);
//                        } catch (Exception e) {
//                            NotificationError.show(e);
//                        }
//                    } else {
//                        activeTask.setText("No valid task to add");
//                        activeTask.setVisible(false);
//                    }
//
//                    insertBefore.setEnabled(hasActiveTaskProvider);
//                    insertAfter.setEnabled(hasActiveTaskProvider);
//                    insertAsSubtask.setEnabled(hasActiveTaskProvider);
//                    return true;
//                }
//            });
//        }
//
//        @Override
//        protected boolean onBeforeOpenMenu(JsonObject eventDetail) {
//            if (eventDetail.getString("columnId").equals(COLUMN_ID_DRAG_HANDLE)) {
//                return false;
//            }
//            return super.onBeforeOpenMenu(eventDetail);
//        }
//    }
//
//    private Div gridOptionsMenu() {
//        MenuBar menuBar = new MenuBar();
//        menuBar.setWidth(ICON_COL_WIDTH_S);
//        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
//
//        menuBar.getElement().setProperty("closeOn", "vaadin-overlay-outside-click");
//
//        InlineIconButton menuIcon = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
//        SubMenu subMenu = menuBar.addItem(menuIcon)
//                .getSubMenu();
//
//        MenuItem visibilityMenu = subMenu.addItem("Column Visibility");
//
//        TriConsumer<Grid.Column<TaskEntry>, MenuItem, Boolean> toggleVisibility = (column, menuItem, checked) -> {
//            menuItem.setChecked(checked);
//            settings.columnVisibility().put(column.getKey(), checked);
//            column.setVisible(checked);
//        };
//
//        VISIBILITY_TOGGLEABLE_COLUMNS.forEach(
//                (columnKey) -> {
//                    Grid.Column<TaskEntry> column = treeGrid.getColumnByKey(columnKey);
//                    MenuItem menuItem = visibilityMenu.getSubMenu().addItem(columnKey);
//                    menuItem.setCheckable(true);
//
//                    toggleVisibility.accept(column, menuItem, settings.columnVisibility().get(columnKey));
//                    menuItem.addClickListener(e -> toggleVisibility.accept(
//                            column, menuItem, menuItem.isChecked()));
//
//                    RetainOpenedMenuItemDecorator.keepOpenOnClick(menuItem);
//                }
//        );
//
//        MenuItem descriptionsView = subMenu.addItem("Descriptions");
//
//        descriptionsView.getSubMenu().addItem("Expand Existing", event ->
//            settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.IF_PRESENT));
//        descriptionsView.getSubMenu().addItem("Expand All", event ->
//                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.ALL));
//        descriptionsView.getSubMenu().addItem("Hide All", event ->
//                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.NONE));
//
//        return new Div(menuBar);
//    }
//}
