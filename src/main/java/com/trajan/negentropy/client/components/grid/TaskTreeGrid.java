package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.sessionlogger.SessionLogged;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.components.grid.subcomponents.NestedTaskTabs;
import com.trajan.negentropy.client.components.grid.subcomponents.RetainOpenedMenuItemDecorator;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskNodeDisplay;
import com.trajan.negentropy.client.session.DescriptionViewDefaultSetting;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProviderFactory;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextAreaVariant;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Scope("prototype")
@Accessors(fluent = true)
@Getter
public abstract class TaskTreeGrid<T extends HasTaskData> extends Div implements TaskNodeDisplay, SessionLogged {
    @Autowired protected SessionLoggerFactory loggerFactory;
    protected SessionLogger log;

    @Autowired protected UIController controller;
    @Autowired protected UserSettings settings;

    @Autowired protected DurationEstimateValueProviderFactory<T> durationEstimateValueProviderFactory;

    protected TreeGrid<T> treeGrid;
    protected Editor<T> editor;
    protected T draggedItem;

    protected NestedTaskTabs nestedTabs;
    protected HorizontalLayout topBar;

    protected LinkedHashMap<ColumnKey, Boolean> visibleColumns;
    protected Grid.Column<T> editColumn;

    protected FormLayout editHeaderLayout;
    protected List<Checkbox> editCheckboxes;

    protected SelectionMode selectionMode;

    protected abstract TreeGrid<T> createGrid();

    @PostConstruct
    public void init() {
        log = getLogger(this.getClass());
    }

    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        treeGrid = createGrid();
        this.visibleColumns = visibleColumns;

        editor = treeGrid.getEditor();

        topBar = new HorizontalLayout();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setWidthFull();

        this.add(topBar);

        editHeaderLayout = new FormLayout();
        editCheckboxes = new LinkedList<>();

        setSelectionMode(selectionMode);
        initReadColumns();
        initEditColumns();
        initDetails();
        configureDragAndDrop();
        configureAdditionalEvents();
        configureAdditionalTopBarComponents().forEach(topBar::add);

        FormLayout middleBar = configureMiddleBar();
        if (middleBar != null) {
            this.add(middleBar);
            middleBar.setWidthFull();
        }

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        treeGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        this.add(treeGrid);
    }

    private void initReadColumns() {
        visibleColumns.forEach((columnKey, visible) -> {
            if (visible != null && visible) {
                initColumn(columnKey);
            }
        });
    }

    protected void setMultiEditCheckboxHeader(
            Grid.Column<T> column,
            Function<T, Boolean> getter,
            Function<Boolean, Data> inputFunction,
            Function<T, ID> idFunction) {

        if (!selectionMode.equals(SelectionMode.NONE)) {
            Checkbox checkbox = new Checkbox(column.getKey());

            editHeaderLayout.add(checkbox);

            editCheckboxes.add(checkbox);
            treeGrid.addSelectionListener(event -> {
                log.debug("Selection changed");
                if (event.isFromClient()) {
                    Set<T> data = treeGrid.getSelectedItems();

                    if (data.isEmpty()) {
                        return;
                    }

                    boolean firstRequiredValue = data.stream()
                            .map(getter)
                            .findFirst().get();

                    boolean allSame = data.stream()
                            .allMatch(t -> getter.apply(t) == firstRequiredValue);

                    log.debug("All same: {}", allSame);
                    if (allSame) {
                        checkbox.setValue(firstRequiredValue);
                        checkbox.setIndeterminate(false);
                    } else {
                        checkbox.setIndeterminate(true);
                    }
                }
            });

            checkbox.addValueChangeListener(event -> {
                if (event.isFromClient()) {
                    Set<T> toUpdate = treeGrid.getSelectedItems();

                    if (!toUpdate.isEmpty()) {
                        controller.requestChange(new MultiMergeChange<>(
                                inputFunction.apply(checkbox.getValue()),
                                toUpdate.stream()
                                        .map(idFunction)
                                        .toList()));
                    }
                }
            });
        }
    }

    private void initColumn(ColumnKey columnKey) {
        if (treeGrid.getColumnByKey(columnKey.toString()) == null) {
            switch (columnKey) {
                case DRAG_HANDLE -> {
                    SerializableConsumer<T> onDown = t -> {
                        treeGrid.setRowsDraggable(true);
                        draggedItem = t;
                    };

                    treeGrid.addColumn(LitRenderer.<T>of(
                            GridUtil.inlineLineAwesomeIconLitExpression(LineAwesomeIcon.GRIP_LINES_VERTICAL_SOLID,
                                    "@mousedown=\"${onDown}\" @touchstart=\"${onDown}\" "))
                                    .withFunction("onDown", onDown))
                            .setKey(ColumnKey.DRAG_HANDLE.toString())
                            .setWidth(GridUtil.ICON_COL_WIDTH_L)
                            .setFrozen(true)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER)
                            .setId(K.COLUMN_ID_DRAG_HANDLE);
                }

                case NAME -> treeGrid.addHierarchyColumn(
                        t -> t.task().name())
                        .setKey(ColumnKey.NAME.toString())
                        .setHeader(ColumnKey.NAME.toString())
                        .setWidth("150px")
                        .setFrozen(true)
                        .setFlexGrow(1);

                case REQUIRED -> {
                    Grid.Column<T> requiredColumn = treeGrid.addColumn(LitRenderer.<T>of(
                                            GridUtil.inlineVaadinIconLitExpression("exclamation-circle-o",
                                                    "?active=\"${item.required}\" "))
                                    .withFunction("onClick", t ->
                                            controller.requestChangeAsync(new MergeChange<>(
                                                    new Task(t.task().id())
                                                            .required(!t.task().required())),
                                                    this))
                                    .withProperty("required", t ->
                                            t.task().required()))
                            .setKey(ColumnKey.REQUIRED.toString())
                            .setHeader(GridUtil.headerIcon(VaadinIcon.EXCLAMATION))
                            .setWidth(GridUtil.ICON_COL_WIDTH_L)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    setMultiEditCheckboxHeader(requiredColumn,
                            t -> t.task().required(),
                            (toggle) ->
                                    new Task().required(toggle),
                            t -> t.task().id());
                }

                case PROJECT -> {
                    Grid.Column<T> projectColumn = treeGrid.addColumn(LitRenderer.<T>of(
                                            GridUtil.inlineVaadinIconLitExpression("file-tree",
                                                    ("?active=\"${item.project}\" ")))
                                    .withFunction("onClick", t ->
                                            controller.requestChangeAsync(new MergeChange<>(
                                                    new Task(t.task().id())
                                                            .project(!t.task().project())),
                                                    this))
                                    .withProperty("project", t ->
                                            t.task().project()))
                            .setKey(ColumnKey.PROJECT.toString())
                            .setHeader(GridUtil.headerIcon(VaadinIcon.FILE_TREE))
                            .setWidth(GridUtil.ICON_COL_WIDTH_L)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    setMultiEditCheckboxHeader(projectColumn,
                            t -> t.task().project(),
                            (toggle) ->
                                    new Task().project(toggle),
                            t -> t.task().id());
                }

                case TAGS_COMBO -> treeGrid.addColumn(new ComponentRenderer<>(
                        t -> {
                            TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
                            tagComboBox.setWidthFull();
                            tagComboBox.setClassName("grid-combo-box");
                            tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                            tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                            tagComboBox.setValue(t.task().tags());
                            tagComboBox.addValueChangeListener(event ->
                                    controller.requestChangeAsync(new MergeChange<>(
                                            new Task(t.task().id())
                                                    .tags(event.getValue())),
                                            this));
                            return tagComboBox;
                        }))
                        .setKey(ColumnKey.TAGS_COMBO.toString())
                        .setHeader(ColumnKey.TAGS_COMBO.toString())
                        .setAutoWidth(true)
                        .setFlexGrow(1)
                        .setClassName("tag-column");

                case TAGS -> treeGrid.addColumn(
                                t -> t.task().tags().stream()
                                        .map(Tag::name)
                                        .collect(Collectors.joining(" | ")))
                        .setKey(ColumnKey.TAGS.toString())
                        .setHeader(ColumnKey.TAGS.toString())
                        .setFlexGrow(1);

                case DESCRIPTION -> treeGrid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("eye",
                                "?active=\"${item.hasDescription}\""))
                                .withFunction("onClick", t ->
                                        treeGrid.setDetailsVisible(
                                                t,
                                                !treeGrid.isDetailsVisible(t)))
                                .withProperty("hasDescription", t ->
                                        !t.task().description().isBlank()))
                        .setKey(ColumnKey.DESCRIPTION.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.CLIPBOARD_TEXT))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                case DURATION -> treeGrid.addColumn(
                        durationEstimateValueProviderFactory.get(
                                DurationEstimateValueProvider.DurationType.TASK_DURATION))
                        .setKey(ColumnKey.DURATION.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.CLOCK))
                        .setWidth(GridUtil.DURATION_COL_WIDTH)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                case NET_DURATION -> {
                    Button columnHeaderButton = new Button(new Span(
                            GridUtil.headerIconPrimary(VaadinIcon.FILE_TREE_SUB),
                            GridUtil.headerIconPrimary(VaadinIcon.CLOCK)));
                    columnHeaderButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

                    DurationEstimateValueProvider<T> provider = durationEstimateValueProviderFactory.get(
                            DurationEstimateValueProvider.DurationType.NET_DURATION);
                    columnHeaderButton.addSingleClickListener(event -> {
                        log.debug("Toggle net duration format");
                        provider.toggleFormat();
                        treeGrid.getDataProvider().refreshAll();
                    });
                    columnHeaderButton.addClassName(K.ICON_COLOR_COMPLEMENTARY);

                    treeGrid.addColumn(provider)
                            .setKey(ColumnKey.NET_DURATION.toString())
                            .setHeader(columnHeaderButton)
                            .setWidth(GridUtil.DURATION_COL_WIDTH)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);
                }

                case EDIT -> editColumn = treeGrid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("edit",
                                "active"))
                                .withFunction("onClick", t -> {
                                    if (editor.isOpen()) {
                                        editor.cancel();
                                    }
                                    editor.editItem(t);
                                }))
                        .setKey(ColumnKey.EDIT.toString())
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER);

                default -> this.initAdditionalReadColumns(columnKey);
            }
        }

        treeGrid.setSortableColumns();
        treeGrid.setColumnReorderingAllowed(true);
        this.setColumnSortOrder();
        this.setPartNameGenerator();
    }

    protected abstract void initAdditionalReadColumns(ColumnKey columnKey);

    protected abstract List<ColumnKey> getPossibleColumns();

    private void setColumnSortOrder() {
        List<String> columns = treeGrid.getColumns().stream()
                .map(Grid.Column::getKey)
                .toList();
        List<Grid.Column<T>> columnOrder = new LinkedList<>();

        getPossibleColumns().forEach(column -> {
            if(columns.contains(column.toString())) {
                columnOrder.add(treeGrid.getColumnByKey(column.toString()));
            }
        });

        treeGrid.setColumnOrder(columnOrder);
    }

    protected abstract void setPartNameGenerator();

    protected abstract AbstractTaskFormLayout getTaskFormLayout(T t);

    protected FormLayout detailsForm(T t) {
        AbstractTaskFormLayout form = getTaskFormLayout(t);
        form.saveAsLastCheckbox().setVisible(false);
        form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
                LumoUtility.BoxSizing.BORDER);
        form.clearEventListener().afterClear(() -> editor.cancel());
        form.taskNodeProvider().afterSuccessfulSave(() -> editor.save());

        editor.setBinder(this.setEditorBinder(form));

        return form;
    }

    protected abstract Binder<T> setEditorBinder(AbstractTaskFormLayout form);

    protected HorizontalLayout detailsDescription(T t) {
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

        descriptionArea.setValue(t.task().description());
        descriptionSaveButton.setVisible(false);
        descriptionCancelButton.setVisible(false);

        Consumer<Boolean> toggleEditing = editing -> {
            descriptionSaveButton.setVisible(editing);
            descriptionCancelButton.setVisible(editing);
        };

        Runnable saveDescription = () -> {
            toggleEditing.accept(false);

            controller.requestChange(new MergeChange<>(
                    new Task(t.task().id())
                            .description(descriptionArea.getValue())));
        };

        Runnable cancelEditingDescription = () -> {
            toggleEditing.accept(false);

            descriptionArea.setValue(t.task().description());
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
        ComponentRenderer<Component, T> detailsRenderer = new ComponentRenderer<>(t -> {
            Predicate<T> isBeingEdited = gt -> gt.equals(editor.getItem());

            if (isBeingEdited.test(t) && visibleColumns.get(ColumnKey.EDIT)) {
                return detailsForm(t);
            } else {
                return detailsDescription(t);
            }
        });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
        treeGrid.setDetailsVisibleOnClick(false);
    }

    protected void initEditColumns() {
        if (visibleColumns.getOrDefault(ColumnKey.EDIT, false)) {
            Editor<T> editor = treeGrid.getEditor();
            this.setEditorSaveListener();

            editor.setBuffered(true);

            InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
            check.addClickListener(e -> {
                treeGrid.setDetailsVisible(editor.getItem(), false);
                editor.save();
            });

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

    protected abstract Registration setEditorSaveListener();

    protected abstract void configureDragAndDrop();

    protected abstract void configureAdditionalEvents();

    protected void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        treeGrid.setSelectionMode(selectionMode);
        editHeaderLayout.setVisible(selectionMode.equals(SelectionMode.MULTI));
    }

    protected Collection<Component> configureAdditionalTopBarComponents() {
        return List.of();
    }

    protected FormLayout configureMiddleBar() {
        return null;
    }

    protected void setColumnVisibility(ColumnKey columnKey, boolean visible) {
        visibleColumns.put(columnKey, visible);
    }

    protected boolean getColumnVisibility(ColumnKey columnKey) {
        return visibleColumns.getOrDefault(columnKey, false);
    }

    protected Div gridOptionsMenu(List<ColumnKey> possibleColumns) {
        MenuBar menuBar = new MenuBar();
        menuBar.setWidth(GridUtil.ICON_COL_WIDTH_S);
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        menuBar.getElement().setProperty("closeOn", "vaadin-overlay-outside-click");

        InlineIconButton menuIcon = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = menuBar.addItem(menuIcon)
                .getSubMenu();

        if (!this.selectionMode.equals(SelectionMode.NONE)) {
            MenuItem multiSelect = subMenu.addItem("Multi-Select");
            multiSelect.setCheckable(true);
            multiSelect.setChecked(settings.gridSelectionMode().equals(SelectionMode.MULTI));
            multiSelect.addClickListener(e -> {
                settings.gridSelectionMode(multiSelect.isChecked()
                        ? SelectionMode.MULTI : SelectionMode.SINGLE);
                this.setSelectionMode(settings.gridSelectionMode());
            });
        }

        MenuItem visibilityMenu = subMenu.addItem("Column Visibility");

        TriConsumer<ColumnKey, MenuItem, Boolean> toggleVisibility = (columnKey, menuItem, checked) -> {
            menuItem.setChecked(checked);
            this.setColumnVisibility(columnKey, checked);
            Grid.Column<T> column = treeGrid.getColumnByKey(columnKey.toString());
            if (column != null) {
                column.setVisible(checked);
            } else if (checked) {
                initColumn(columnKey);
                setColumnSortOrder();
            }
        };

        possibleColumns.forEach(
                (columnKey) -> {
                    String columnName = columnKey.toString();
                    MenuItem menuItem = visibilityMenu.getSubMenu().addItem(columnName);
                    menuItem.setCheckable(true);

                    toggleVisibility.accept(columnKey, menuItem, this.getColumnVisibility(columnKey));

                    menuItem.addClickListener(e -> toggleVisibility.accept(
                            columnKey, menuItem, menuItem.isChecked()));
                    // TODO: Use visibleColumns

                    RetainOpenedMenuItemDecorator.keepOpenOnClick(menuItem);
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
