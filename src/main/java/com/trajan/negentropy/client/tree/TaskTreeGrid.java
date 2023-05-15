package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.TaskEntryFormLayout;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.tree.components.InlineIconButton;
import com.trajan.negentropy.client.tree.components.InlineIconToggleButton;
import com.trajan.negentropy.client.tree.components.NestedTaskTabs;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import elemental.json.JsonObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(fluent = true)
@Getter
public class TaskTreeGrid extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(TaskTreeGrid.class);

    private final TreeViewPresenter presenter;
    private final SessionSettings settings;

    private final TreeGrid<TaskEntry> treeGrid;
    private final NestedTaskTabs nestedTabs;

    private Grid.Column<TaskEntry> dragHandleColumn;
    private Grid.Column<TaskEntry> nameColumn;
    private Grid.Column<TaskEntry> completeColumn;
    private Grid.Column<TaskEntry> recurringColumn;
    private Grid.Column<TaskEntry> tagColumn;
    private Grid.Column<TaskEntry> descriptionColumn;
    private Grid.Column<TaskEntry> taskDurationColumn;
    private Grid.Column<TaskEntry> timeEstimateColumn;
    private Grid.Column<TaskEntry> editColumn;
    private Grid.Column<TaskEntry> deleteColumn;

    private TaskEntry draggedItem;

    private final String DURATION_COL_WIDTH = "50px";
    private final String ICON_COL_WIDTH_S = "31px";
    private final String ICON_COL_WIDTH_L = "35px";

    private Editor<TaskEntry> editor;

    public static final String COLUMN_KEY_DRAG_HANDLE = "Drag Handle";
    public static final String COLUMN_ID_DRAG_HANDLE = "drag-handle-column";
    public static final String COLUMN_KEY_NAME = "Name";
    public static final String COLUMN_KEY_COMPLETE = "Complete";
    public static final String COLUMN_KEY_RECURRING = "Recurring";
    public static final String COLUMN_KEY_TAGS = "Tags";
    public static final String COLUMN_KEY_DESCRIPTION = "Description";
    public static final String COLUMN_KEY_DURATION = "Task Duration";
    public static final String COLUMN_KEY_TIME_ESTIMATE = "Net Time Estimate";
    public static final String COLUMN_KEY_EDIT = "Edit";
    public static final String COLUMN_KEY_DELETE = "Delete";
    public static final List<String> VISIBILITY_TOGGLEABLE_COLUMNS = List.of(
            COLUMN_KEY_DRAG_HANDLE,
            COLUMN_KEY_COMPLETE,
            COLUMN_KEY_RECURRING,
            COLUMN_KEY_TAGS,
            COLUMN_KEY_DESCRIPTION,
            COLUMN_KEY_DURATION,
            COLUMN_KEY_TIME_ESTIMATE,
            COLUMN_KEY_EDIT,
            COLUMN_KEY_DELETE
    );

    public TaskTreeGrid(TreeViewPresenter presenter, SessionSettings settings) {
        this.presenter = presenter;
        this.settings = settings;
        
        this.treeGrid = new TreeGrid<>(TaskEntry.class);
        treeGrid.setDataProvider(presenter.dataProvider());

        this.nestedTabs = new NestedTaskTabs(presenter);
        new TaskTreeContextMenu(treeGrid);
        this.editor = treeGrid.getEditor();

        this.initReadColumns();
        this.initEditColumns();
        this.initDetails();
        this.configureDragAndDrop();
        this.configureSelection();

        nestedTabs.addThemeVariants(TabsVariant.LUMO_SMALL);

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        // TODO: Wrap cell content?
        treeGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);
        this.setPadding(false);

        HorizontalLayout toolbar = new HorizontalLayout(
                nestedTabs,
                visibilityMenu());
        toolbar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbar.setWidthFull();

        this.add(
                toolbar,
                treeGrid);
    }

    private ComponentRenderer<Component, TaskEntry> inlineButtonRenderer(
            VaadinIcon iconWhenTrue, VaadinIcon iconWhenFalse,
            Consumer<TaskEntry> onClick,
            Predicate<TaskEntry> isVisible,
            Predicate<TaskEntry> iconSwap) {
        return new ComponentRenderer<>(entry -> {
            Div div = new Div();
            if (isVisible.test(entry)) {
                InlineIconButton iconA = new InlineIconButton(iconWhenTrue.create());
                InlineIconButton iconB = new InlineIconButton(iconWhenFalse.create());

                iconA.setVisible(!iconSwap.test(entry));
                iconB.setVisible(iconSwap.test(entry));

                iconA.addClickListener(e -> {
                    onClick.accept(entry);
                });

                iconB.addClickListener(e -> {
                    onClick.accept(entry);
                });

                div.add(iconA, iconB);
            }
            return div;
        });
    }

    private void initReadColumns() {
        Consumer<TaskEntry> onDown = entry -> {
            treeGrid.setRowsDraggable(true);
            draggedItem = entry;
        };

        dragHandleColumn = treeGrid.addColumn(
                new ComponentRenderer<>(entry -> {
                    Component dragHandle = LineAwesomeIcon.GRIP_LINES_VERTICAL_SOLID.create();
                    dragHandle.addClassName(K.ICON_COLOR_UNSELECTED);
                    dragHandle.getElement().addEventListener("mousedown", e -> onDown.accept(entry));
                    dragHandle.getElement().addEventListener("touchStart", e -> onDown.accept(entry));
                    return dragHandle;
                }))
                .setKey(COLUMN_KEY_DRAG_HANDLE)
                .setWidth(ICON_COL_WIDTH_L)
                .setFrozen(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);
        dragHandleColumn
                .setId(COLUMN_ID_DRAG_HANDLE);

        nameColumn = treeGrid.addHierarchyColumn(
                        entry -> entry.task().name())
                .setKey(COLUMN_KEY_NAME)
                .setHeader(COLUMN_KEY_NAME)
                .setWidth("150px")
                .setFrozen(true)
                .setFlexGrow(1);

        Icon completeColumnHeaderIcon = VaadinIcon.CHECK_SQUARE_O.create();
        completeColumnHeaderIcon.setSize(K.INLINE_ICON_SIZE);
        completeColumnHeaderIcon.addClassName(K.ICON_COLOR_UNSELECTED);
        completeColumn = treeGrid.addColumn(
                new ComponentRenderer<>(entry -> {
                    InlineIconButton completeOneTime = new InlineIconButton(VaadinIcon.CHECK.create());
                    completeOneTime.addClickListener(event -> presenter.deleteNode(entry));
                    completeOneTime.setEnabled(!entry.task().recurring());
                    completeOneTime.setVisible(!entry.hasChildren());
                    return completeOneTime;
                }))
                .setKey(COLUMN_KEY_COMPLETE)
                .setHeader(completeColumnHeaderIcon)
                .setWidth(ICON_COL_WIDTH_L)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Icon recurringColumnHeaderIcon = VaadinIcon.ROTATE_RIGHT.create();
        recurringColumnHeaderIcon.setSize(K.INLINE_ICON_SIZE);
        recurringColumnHeaderIcon.addClassName(K.ICON_COLOR_UNSELECTED);
        recurringColumn = treeGrid.addColumn(
                new ComponentRenderer<>(entry -> {
                    InlineIconToggleButton isTaskRecurring = new InlineIconToggleButton(
                            VaadinIcon.ROTATE_RIGHT.create());
                    if (entry.task().recurring()) {
                        isTaskRecurring.activate();
                    }
                    isTaskRecurring.onToggle( () ->
                            presenter.updateTask(new Task(entry.task().id())
                                    .recurring(!entry.task().recurring())));
                    return isTaskRecurring;
                }))
                .setKey(COLUMN_KEY_RECURRING)
                .setHeader(recurringColumnHeaderIcon)
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        tagColumn = treeGrid.addColumn(
                new ComponentRenderer<>(entry -> {
                    TagComboBox tagComboBox = new CustomValueTagComboBox(presenter);
                    tagComboBox.setWidthFull();
                    tagComboBox.setClassName("grid-combo-box");
                    tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                    tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                    tagComboBox.setValue(entry.task().tags());
                    tagComboBox.addValueChangeListener(event -> {
                        Task task = new Task(entry.task().id())
                                .tags(event.getValue());
                        presenter.updateTask(task);
                    });
                    return tagComboBox;
                }))
                .setKey(COLUMN_KEY_TAGS)
                .setHeader(COLUMN_KEY_TAGS)
                .setFlexGrow(1);
        tagColumn.setClassName("tag-column");

        Icon descriptionColumnHeaderIcon = VaadinIcon.NOTEBOOK.create();
        descriptionColumnHeaderIcon.setSize(K.INLINE_ICON_SIZE);
        descriptionColumnHeaderIcon.addClassName(K.ICON_COLOR_UNSELECTED);
        descriptionColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.EYE, VaadinIcon.EYE_SLASH,
                        entry -> {
                            treeGrid.setDetailsVisible(
                                    entry,
                                    !treeGrid.isDetailsVisible(entry));
                        },
                        entry -> !entry.task().description().isBlank(),
                        treeGrid::isDetailsVisible))
                .setKey(COLUMN_KEY_DESCRIPTION)
                .setHeader(descriptionColumnHeaderIcon)
                .setWidth(ICON_COL_WIDTH_L)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

//        Grid.Column<TaskEntry> priorityColumn = treeGrid
//                .addColumn(entry ->
//                        entry.link().getPriority())
//                .setHeader("Priority")
//                .setAutoWidth(true)
//                .setFlexGrow(0);

        Icon durationColumnHeaderIcon = VaadinIcon.CLOCK.create();
        durationColumnHeaderIcon.setSize(K.INLINE_ICON_SIZE);
        durationColumnHeaderIcon.addClassNames("unselected-color-icon");
        taskDurationColumn = treeGrid.addColumn(
                        new TimeEstimateValueProvider<>(
                                presenter.queryService(),
                                () -> TimeFormat.DURATION,
                                false))
                .setKey(COLUMN_KEY_DURATION)
                .setHeader(durationColumnHeaderIcon)
                .setWidth(DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Icon clock = VaadinIcon.CLOCK.create();
        clock.setSize(K.INLINE_ICON_SIZE);
        clock.addClassNames("unselected-color-icon");

        Icon tree = VaadinIcon.FILE_TREE_SUB.create();
        tree.setSize(K.INLINE_ICON_SIZE);
        tree.addClassNames("unselected-color-icon");

        HorizontalLayout timeEstimateColumnHeader = new HorizontalLayout(tree, clock);
        timeEstimateColumnHeader.setSpacing(false);
        timeEstimateColumnHeader.setJustifyContentMode(JustifyContentMode.CENTER);
        timeEstimateColumnHeader.setSizeFull();
        timeEstimateColumn = treeGrid.addColumn(
                        new TimeEstimateValueProvider<>(
                                presenter.queryService(),
                                () -> TimeFormat.DURATION,
                                true))
                .setKey(COLUMN_KEY_TIME_ESTIMATE)
                .setHeader(timeEstimateColumnHeader)
                .setWidth(DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        editColumn = treeGrid.addColumn(
                new ComponentRenderer<>(entry -> new InlineIconButton(
                        VaadinIcon.EDIT.create(),
                        () -> {
                            if (editor.isOpen()) {
                                editor.cancel();
                            }
                            editor.editItem(entry);
                            treeGrid.setDetailsVisible(entry, true);
                        }))
                )
                .setKey(COLUMN_KEY_EDIT)
                .setWidth(ICON_COL_WIDTH_L)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        List<InlineIconButton> deleteIcons = new ArrayList<>();
        InlineIconToggleButton trashIconButton = new InlineIconToggleButton(
                VaadinIcon.EYE_SLASH.create(),
                VaadinIcon.TRASH.create());
        trashIconButton.onToggle(
                () -> {
                    deleteIcons.forEach(icon -> icon.setVisible(trashIconButton.activated()));
                });
        trashIconButton.activatedIcon().addClassName(K.ICON_COLOR_ERROR);
        trashIconButton.deactivatedIcon().addClassName(K.ICON_COLOR_ERROR);
        deleteColumn = treeGrid.addColumn(
                new ComponentRenderer<Component, TaskEntry>(entry -> {
                    Div div = new Div();
                    if(!entry.hasChildren()) {
                        InlineIconButton iconButton = new InlineIconButton(VaadinIcon.TRASH.create());
                        iconButton.getIcon().addClassName(K.ICON_COLOR_ERROR);
                        iconButton.addClickListener(event -> presenter.deleteNode(entry));
                        deleteIcons.add(iconButton);
                        div.add(iconButton);
                        iconButton.setVisible(trashIconButton.activated());
                    }
                    return div;
                }))
                .setKey(COLUMN_KEY_DELETE)
                .setHeader(trashIconButton)
                .setWidth(ICON_COL_WIDTH_L)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        treeGrid.setSortableColumns();

        treeGrid.setPartNameGenerator(entry -> {
            if (entry.task().recurring()) {
                return K.GRID_PARTNAME_RECURRING;
            } else {
                return K.GRID_PARTNAME_NON_RECURRING;
            }
        });
    }

    private void initDetails() {
        ComponentRenderer<Component, TaskEntry> detailsRenderer = new ComponentRenderer<>(entry -> {
            Predicate<TaskEntry> isBeingEdited = ntry -> ntry.equals(editor.getItem());

            if (isBeingEdited.test(entry)) {
                TaskEntryFormLayout form = new TaskEntryFormLayout(presenter, entry);
                form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
                        LumoUtility.BoxSizing.BORDER);
                form.onClear(() -> editor.cancel());
                form.onSave(() -> editor.save());

                editor.setBinder(form.binder());

                return form;
            } else {
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
                AtomicBoolean editing = new AtomicBoolean(false);

                Runnable toggleEditing = () -> {
                    boolean toggle = editing.get();
                    descriptionArea.setReadOnly(toggle);
                    descriptionSaveButton.setVisible(!toggle);
                    descriptionCancelButton.setVisible(!toggle);
                    editing.set(!toggle);
                };

                Runnable saveDescription = () -> {
                    toggleEditing.run();

                    presenter.updateTask(new Task(entry.task().id())
                            .description(descriptionArea.getValue()));
                };

                Runnable cancelEditingDescription = () -> {
                    toggleEditing.run();

                    descriptionArea.setValue(entry.task().description());
                };

                descriptionArea.getElement().addEventListener("mouseup", e -> toggleEditing.run());
                descriptionArea.getElement().addEventListener("touchend", e -> toggleEditing.run());

                descriptionSaveButton.addClickListener(e -> saveDescription.run());
                descriptionCancelButton.addClickListener(e -> cancelEditingDescription.run());

                Shortcuts.addShortcutListener(container,
                        saveDescription::run,
                        Key.ENTER);

                Shortcuts.addShortcutListener(container,
                        cancelEditingDescription::run,
                        Key.ESCAPE);

                return container;
            }
        });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
    }

    private void initEditColumns() {
        Editor<TaskEntry> editor = treeGrid.getEditor();
        editor.addSaveListener(e -> presenter.updateTask(e.getItem()));

        editor.setBuffered(true);

        InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
        check.addClickListener(e -> editor.save());

        AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());

        editor.addOpenListener(e -> {
            escapeListener.get().ifPresent(Registration::remove);
            enterListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
                    editor::save,
                    Key.ENTER)));
        });

        editor.addCloseListener(e -> {
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

    private void configureDragAndDrop() {
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        // Drag start is managed by the dragHandleColumn

        treeGrid.addDropListener(event -> {
            logger.debug(draggedItem + " dropped onto " + event.getDropTargetItem().orElseThrow().task().name());
            if (event.getDropTargetItem().isPresent()) {
                TaskEntry target = event.getDropTargetItem().get();
                if (!draggedItem.equals(target)) {
                    switch (event.getDropLocation()) {
                        case ABOVE -> {
                            presenter.moveNodeBefore(
                                    draggedItem,
                                    target);
                        }
                        case BELOW -> {
                            presenter.moveNodeAfter(
                                    draggedItem,
                                    target);
                        }
                        case ON_TOP -> presenter.moveNodeInto(
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

    private boolean isIOS() {
        WebBrowser webBrowser = VaadinSession.getCurrent().getBrowser();
        return webBrowser.isIPhone();
    }

    private void configureSelection() {
        treeGrid.setSelectionMode(Grid.SelectionMode.NONE);

        if (this.isIOS()) {
            final AtomicReference<LocalDateTime> firstClickTime = new AtomicReference<>();
            final AtomicBoolean waitingSecondClick = new AtomicBoolean(false);

            int millisecondInterval = 500;

            treeGrid.addItemClickListener(e -> {
                if (!waitingSecondClick.get()) {
                    firstClickTime.set(LocalDateTime.now());
                    waitingSecondClick.set(true);

                    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                    executor.schedule(() -> waitingSecondClick.set(false), millisecondInterval, TimeUnit.MILLISECONDS);

                    executor.shutdown();
                } else {
                    waitingSecondClick.set(false);

                    if (LocalDateTime.now().compareTo(firstClickTime.get()) < millisecondInterval) {
                        if (e.getItem() != null) {
                            nestedTabs.onSelectNewRootEntry(e.getItem());
                        }
                    }
                }
            });
        } else {
            treeGrid.addItemDoubleClickListener(e -> {
                if (e.getItem() != null) {
                    nestedTabs.onSelectNewRootEntry(e.getItem());
                }
            });
        }
    }

    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {

        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
            super(grid);

            GridMenuItem<TaskEntry> name = addItem("");
            name.setEnabled(false);
            name.addClassName(K.COLOR_PRIMARY);

            add(new Hr());

            GridMenuItem<TaskEntry> insertBefore = addItem("Add before", e -> e.getItem().ifPresent(
                    presenter::addTaskFromActiveProviderBefore
            ));

            GridMenuItem<TaskEntry> insertAfter = addItem("Add after", e -> e.getItem().ifPresent(
                    presenter::addTaskFromActiveProviderAfter
            ));

            GridMenuItem<TaskEntry> insertAsSubtask = addItem("Add as subtask", e -> e.getItem().ifPresent(
                    presenter::addTaskFromActiveProviderAsChild
            ));

            add(new Hr());

            addItem("Remove", e -> e.getItem().ifPresent(
                    presenter::deleteNode
            ));

            // Do not show context menu when header is clicked
            setDynamicContentHandler(entry -> {
                if (entry == null) {
                    return false;
                } else {
                    boolean hasActiveTaskProvider =
                            presenter.activeTaskProvider() != null
                                    && presenter.activeTaskProvider().hasValidTask().success();

                    if (hasActiveTaskProvider) {
                        try {
                            presenter.activeTaskProvider().getTask().ifPresentOrElse(
                                    task -> name.setText(task.name()), () -> {
                                    });
                        } catch (Exception e) {
                            NotificationError.show(e);
                        }
                    } else {
                        name.setText("No valid task to add");
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

    private Div visibilityMenu() {
        MenuBar menuBar = new MenuBar();
        menuBar.setWidth(ICON_COL_WIDTH_S);
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        InlineIconButton menuIcon = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = menuBar.addItem(menuIcon)
                .getSubMenu();

        MenuItem visibilityDivider = subMenu.addItem("Column Visibility");
        visibilityDivider.setEnabled(false);
        visibilityDivider.addClassName(K.COLOR_PRIMARY);

        TriConsumer<Grid.Column<TaskEntry>, MenuItem, Boolean> toggleVisibility = (column, menuItem, checked) -> {
            menuItem.setChecked(checked);
            settings.columnVisibility().put(column.getKey(), checked);
            column.setVisible(checked);
        };

        VISIBILITY_TOGGLEABLE_COLUMNS.forEach(
                (columnKey) -> {
                    Grid.Column<TaskEntry> column = treeGrid.getColumnByKey(columnKey);
                    MenuItem menuItem = subMenu.addItem(columnKey);
                    menuItem.setCheckable(true);

                    toggleVisibility.accept(column, menuItem, settings.columnVisibility().get(columnKey));
                    menuItem.addClickListener(e -> toggleVisibility.accept(
                            column, menuItem, menuItem.isChecked()));
                }
        );

        return new Div(menuBar);
    }
}
