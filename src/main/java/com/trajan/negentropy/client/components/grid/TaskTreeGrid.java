package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.components.RetainOpenedMenuItemDecorator;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.HasTaskData;
import com.trajan.negentropy.client.session.DescriptionViewDefaultSetting;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.components.grid.components.InlineIconButton;
import com.trajan.negentropy.client.components.grid.components.NestedTaskTabs;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
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
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SpringComponent
@RouteScope
@Scope("prototype")
@Slf4j
@Accessors(fluent = true)
@Getter
public abstract class TaskTreeGrid<T extends HasTaskData> extends Div {
    @Autowired protected ClientDataController controller;
    @Autowired protected UserSettings settings;

    protected TreeGrid<T> treeGrid;
    protected Editor<T> editor;
    protected T draggedItem;

    protected NestedTaskTabs nestedTabs;
    protected HorizontalLayout topBar;

    protected LinkedHashMap<String, Boolean> visibleColumns;
    protected Grid.Column<T> editColumn;

    protected static final String DURATION_COL_WIDTH = "60px";
    protected static final String ICON_COL_WIDTH_S = "31px";
    protected static final String ICON_COL_WIDTH_L = "35px";

    protected Icon headerIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.setSize(K.INLINE_ICON_SIZE);
        icon.addClassName(K.ICON_COLOR_GRAYED);
        return icon;
    }

    protected String inlineLineAwesomeIconLitExpression(LineAwesomeIcon lineAwesomeIcon, String attributes) {
        return "<span class=\"grid-icon-lineawesome\" " + attributes + " style=\"-webkit-mask-position: var(--mask-position); display: inline-block; -webkit-mask-repeat: var(--mask-repeat); vertical-align: middle; --mask-repeat: no-repeat; background-color: currentcolor; --_size: var(--lumo-icon-size-m); flex: 0 0 auto; width: var(--_size); --mask-position: 50%; -webkit-mask-image: var(--mask-image); " +
                "--mask-image: url('line-awesome/svg/" + lineAwesomeIcon.getSvgName() + ".svg'); height: var(--_size);\"></span>";
    }

    protected String inlineVaadinIconLitExpression(String iconName, String attributes) {
        return "<vaadin-icon class=\"grid-icon-vaadin\" icon=\"vaadin:" + iconName + "\" " +
                "@click=\"${onClick}\" " +
                attributes + "></vaadin-icon>";
    }

    protected abstract TreeGrid<T> createGrid();
    public void init(LinkedHashMap<String, Boolean> visibleColumns) {
        treeGrid = createGrid();
        this.visibleColumns = visibleColumns;

        this.editor = treeGrid.getEditor();

        topBar = new HorizontalLayout();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setWidthFull();

        this.initReadColumns();
        this.initEditColumns();
        this.initDetails();
        this.configureDragAndDrop();
        this.configureAdditionalEvents();
        this.configureSelectionMode();
        this.configureAdditionalTobBarComponents().forEach(topBar::add);

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        treeGrid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        this.add(topBar, treeGrid);
    }

    private void initReadColumns() {
        visibleColumns.forEach((column, visible) -> {
            if (visible != null && visible) {
                switch (column) {
                    case K.COLUMN_KEY_DRAG_HANDLE -> {
                        SerializableConsumer<T> onDown = t -> {
                            treeGrid.setRowsDraggable(true);
                            draggedItem = t;
                        };

                        treeGrid.addColumn(LitRenderer.<T>of(
                                inlineLineAwesomeIconLitExpression(LineAwesomeIcon.GRIP_LINES_VERTICAL_SOLID,
                                        "@mousedown=\"${onDown}\" @touchstart=\"${onDown}\" "))
                                        .withFunction("onDown", onDown))
                                .setKey(K.COLUMN_KEY_DRAG_HANDLE)
                                .setWidth(ICON_COL_WIDTH_L)
                                .setFrozen(true)
                                .setFlexGrow(0)
                                .setTextAlign(ColumnTextAlign.CENTER)
                                .setId(K.COLUMN_ID_DRAG_HANDLE);
                    }

                    case K.COLUMN_KEY_NAME -> treeGrid.addHierarchyColumn(
                            t -> t.task().name())
                            .setKey(K.COLUMN_KEY_NAME)
                            .setHeader(K.COLUMN_KEY_NAME)
                            .setWidth("150px")
                            .setFrozen(true)
                            .setFlexGrow(1);

                    case K.COLUMN_KEY_BLOCK -> treeGrid.addColumn(LitRenderer.<T>of(
                            inlineVaadinIconLitExpression("bookmark-o",
                                    "?active=\"${item.block}\" "))
                                    .withFunction("onClick", t ->
                                            controller.updateTask(t.task()
                                                    .block(!t.task().block())))
                                    .withProperty("block", t ->
                                            t.task().block()))
                            .setKey(K.COLUMN_KEY_BLOCK)
                            .setHeader(headerIcon(VaadinIcon.BOOKMARK))
                            .setWidth(ICON_COL_WIDTH_L)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    case K.COLUMN_KEY_TAGS -> treeGrid.addColumn(new ComponentRenderer<>(
                            t -> {
                                TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
                                tagComboBox.setWidthFull();
                                tagComboBox.setClassName("grid-combo-box");
                                tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                                tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                                tagComboBox.setValue(t.task().tags());
                                tagComboBox.addValueChangeListener(event -> {
                                    Task task = new Task(t.task().id())
                                            .tags(event.getValue());
                                    controller.updateTask(task);
                                });
                                return tagComboBox;
                            }))
                            .setKey(K.COLUMN_KEY_TAGS)
                            .setHeader(K.COLUMN_KEY_TAGS)
                            .setFlexGrow(1)
                            .setClassName("tag-column");

                    case K.COLUMN_KEY_DESCRIPTION -> treeGrid.addColumn(LitRenderer.<T>of(
                            inlineVaadinIconLitExpression("eye",
                                    "?active=\"${item.hasDescription}\""))
                                    .withFunction("onClick", t ->
                                            treeGrid.setDetailsVisible(
                                                    t,
                                                    !treeGrid.isDetailsVisible(t)))
                                    .withProperty("hasDescription", t ->
                                            !t.task().description().isBlank()))
                            .setKey(K.COLUMN_KEY_DESCRIPTION)
                            .setHeader(headerIcon(VaadinIcon.CLIPBOARD_TEXT))
                            .setWidth(ICON_COL_WIDTH_L)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    case K.COLUMN_KEY_DURATION -> treeGrid.addColumn(new DurationEstimateValueProvider<>(
                            controller.queryService(),
                                    () -> TimeFormat.DURATION,
                                    false))
                            .setKey(K.COLUMN_KEY_DURATION)
                            .setHeader(headerIcon(VaadinIcon.CLOCK))
                            .setWidth(DURATION_COL_WIDTH)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    case K.COLUMN_KEY_TIME_ESTIMATE -> {
                        HorizontalLayout timeEstimateColumnHeader = new HorizontalLayout(
                                headerIcon(VaadinIcon.FILE_TREE_SUB),
                                headerIcon(VaadinIcon.CLOCK));
                        timeEstimateColumnHeader.setSpacing(false);
                        timeEstimateColumnHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

                        treeGrid.addColumn(new DurationEstimateValueProvider<>(
                                controller.queryService(),
                                        () -> TimeFormat.DURATION,
                                        true))
                                .setKey(K.COLUMN_KEY_TIME_ESTIMATE)
                                .setHeader(timeEstimateColumnHeader)
                                .setWidth(DURATION_COL_WIDTH)
                                .setFlexGrow(0)
                                .setTextAlign(ColumnTextAlign.CENTER);
                    }

                    case K.COLUMN_KEY_EDIT -> editColumn = treeGrid.addColumn(LitRenderer.<T>of(
                            inlineVaadinIconLitExpression("edit",
                                    "active"))
                                    .withFunction("onClick", t -> {
                                        if (editor.isOpen()) {
                                            editor.cancel();
                                        }
                                        editor.editItem(t);
                                    }))
                            .setKey(K.COLUMN_KEY_EDIT)
                            .setWidth(ICON_COL_WIDTH_L)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER);

                    default -> {
                        this.initAdditionalReadColumns(column);
                    }
                }
            }
        });

        treeGrid.setSortableColumns();
        treeGrid.setColumnReorderingAllowed(true);
        this.setPartNameGenerator();
    }

    protected abstract void initAdditionalReadColumns(String column);

    protected abstract void setPartNameGenerator();

    protected abstract AbstractTaskFormLayout getTaskFormLayout(T t);

    protected FormLayout detailsForm(T t) {
        AbstractTaskFormLayout form = getTaskFormLayout(t);
        form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
                LumoUtility.BoxSizing.BORDER);
        form.onClear(() -> editor.cancel());
        form.onSave(() -> editor.save());

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

            controller.updateTask(new Task(t.task().id())
                    .description(descriptionArea.getValue()));
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

            if (isBeingEdited.test(t) && visibleColumns.get(K.COLUMN_KEY_EDIT)) {
                return detailsForm(t);
            } else {
                return detailsDescription(t);
            }
        });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
        treeGrid.setDetailsVisibleOnClick(false);
    }

    protected void initEditColumns() {
        if (visibleColumns.getOrDefault(K.COLUMN_KEY_EDIT, false)) {
            Editor<T> editor = treeGrid.getEditor();
            this.setEditorSaveListener();

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

    protected abstract Registration setEditorSaveListener();

    protected abstract void configureDragAndDrop();

    protected abstract void configureAdditionalEvents();

    protected void configureSelectionMode() {
        treeGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
    }

    protected Collection<Component> configureAdditionalTobBarComponents() {
        return List.of();
    }

    protected BiConsumer<String, Boolean> setColumnVisibility = (column, visible) ->
            visibleColumns.put(column, visible);

    protected Function<String, Boolean> getColumnVisibility = column ->
            visibleColumns.get(column);

    protected Div gridOptionsMenu(List<String> possibleColumns) {
        MenuBar menuBar = new MenuBar();
        menuBar.setWidth(ICON_COL_WIDTH_S);
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        menuBar.getElement().setProperty("closeOn", "vaadin-overlay-outside-click");

        InlineIconButton menuIcon = new InlineIconButton(VaadinIcon.ELLIPSIS_DOTS_V.create());
        SubMenu subMenu = menuBar.addItem(menuIcon)
                .getSubMenu();

        MenuItem visibilityMenu = subMenu.addItem("Column Visibility");

        TriConsumer<Grid.Column<T>, MenuItem, Boolean> toggleVisibility = (column, menuItem, checked) -> {
            menuItem.setChecked(checked);
            this.setColumnVisibility.accept(column.getKey(), checked);
            column.setVisible(checked);
        };

        possibleColumns.forEach(
                (columnKey) -> {
                    Grid.Column<T> column = treeGrid.getColumnByKey(columnKey);
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
