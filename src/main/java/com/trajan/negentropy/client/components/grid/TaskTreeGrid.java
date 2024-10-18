package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.components.grid.subcomponents.NestedTaskTabs;
import com.trajan.negentropy.client.components.grid.subcomponents.RetainOpenedMenuItemDecorator;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.HasRootNode;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.DescriptionViewDefaultSetting;
import com.trajan.negentropy.client.session.TaskEntryDataProvider;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider;
import com.trajan.negentropy.client.util.duration.DurationEstimateValueProvider.DurationType;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter.NestableTaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.trajan.negentropy.model.sync.Change.TagMultiMerge;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridNoneSelectionModel;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Getter
@Benchmark(millisFloor = 10)
public abstract class TaskTreeGrid<T extends HasTaskData> extends AbstractTaskGrid<T> implements HasRootNode {
    private final UILogger log = new UILogger();

    @Autowired protected UIController controller;
    @Autowired protected TaskNetworkGraph taskNetworkGraph;
    @Autowired protected UserSettings settings;

    public MultiSelectTreeGrid<T> grid() {
        return (MultiSelectTreeGrid) grid;
    }
    protected T draggedItem;

    protected NestedTaskTabs nestedTabs;
    protected HorizontalLayout topBar;

    protected TagComboBox multiMergeTagComboBox;

    protected SelectionMode selectionMode;

    @Override
    protected abstract MultiSelectTreeGrid<T> createGrid();

    @Override
    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        configureTopBar();

        super.init(visibleColumns, selectionMode);

        configureDragAndDrop();
        configureAdditionalEvents();
        configureAdditionalTopBarComponents().forEach(topBar::add);
    }

    protected void configureTopBar() {
        topBar = new HorizontalLayout();
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setWidthFull();
    }

    @Override
    protected void addComponents() {
        this.add(topBar);

        Component middleBar = configureMiddleBar();
        if (middleBar != null) {
            this.add(middleBar);
        }

        this.add(grid);
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
            grid.addSelectionListener(event -> {
                log.trace("Selection changed");
                if (event.isFromClient()) {
                    Set<T> data = grid.getSelectedItems();

                    if (data.isEmpty()) {
                        return;
                    }

                    boolean firstRequiredValue = data.stream()
                            .map(getter)
                            .findFirst().get();

                    boolean allSame = data.stream()
                            .allMatch(t -> getter.apply(t) == firstRequiredValue);

                    log.trace("All same: {}", allSame);
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
                    Set<T> toUpdate = grid.getSelectedItems();

                    if (!toUpdate.isEmpty()) {
                        controller.requestChangeAsync(new MultiMergeChange<>(
                                inputFunction.apply(checkbox.getValue()),
                                toUpdate.stream()
                                        .map(idFunction)
                                        .toList()));
                    }
                }
            });
        }
    }

    private void addMultiMergeTagComboBox() {
        if (multiMergeTagComboBox == null && !(grid.getSelectionModel() instanceof GridNoneSelectionModel<T>)) {

            BiConsumer<Set<Tag>, Set<Tag>> tagMultiMerge = (old, updated) -> {
                Set<TagID> removedFromOriginal = new HashSet<>(old)
                        .stream()
                        .filter(tag -> !updated.contains(tag))
                        .map(Tag::id)
                        .collect(Collectors.toSet());
                Set<TagID> addedToOriginal = new HashSet<>(updated)
                        .stream()
                        .filter(tag -> !old.contains(tag))
                        .map(Tag::id)
                        .collect(Collectors.toSet());
                Set<TaskID> selectedTasks = grid.getSelectedItems()
                        .stream()
                        .map(t -> t.task().id())
                        .collect(Collectors.toSet());
                controller.requestChangeAsync(new TagMultiMerge(
                        addedToOriginal,
                        removedFromOriginal,
                        selectedTasks));
            };

            multiMergeTagComboBox = new CustomValueTagComboBox(controller, tagMultiMerge);
            multiMergeTagComboBox.setWidthFull();
            multiMergeTagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
            multiMergeTagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
            multiMergeTagComboBox.setPlaceholder("Tags of all selected items");

            grid.addSelectionListener(e -> {
                Set<T> selectedItems = e.getAllSelectedItems();
                if (selectedItems.isEmpty()) {
                    multiMergeTagComboBox.clear();
                } else {
                    Collection<Tag> commonTags = taskNetworkGraph.taskTagMap().get(selectedItems.iterator().next()
                            .task().id());
                    for (T item : selectedItems) {
                        Collection<Tag> tags = taskNetworkGraph.taskTagMap().get(item.task().id());
                        commonTags.retainAll(tags);
                    }
                    multiMergeTagComboBox.setValue(commonTags);
                }
            });

            multiMergeTagComboBox.addValueChangeListener(e -> {
                if (e.isFromClient()) {
                    Set<Tag> oldTags = e.getOldValue();
                    Set<Tag> newTags = e.getValue();
                    tagMultiMerge.accept(oldTags, newTags);
                }
            });

            editHeaderLayout.add(multiMergeTagComboBox);
            editHeaderLayout.setColspan(multiMergeTagComboBox, 99);
        } else if (grid.getSelectionModel() instanceof GridNoneSelectionModel<T>
                && multiMergeTagComboBox != null) {
            editHeaderLayout.remove(multiMergeTagComboBox);
            multiMergeTagComboBox = null;
        }
    }

    public Column<T> configureLimitColumn(Column<T> limitColumn) {
        return limitColumn.setKey(ColumnKey.LIMIT.toString())
                .setHeader(GridUtil.headerIcon(VaadinIcon.STOPWATCH))
                .setAutoWidth(false)
                .setWidth(GridUtil.DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setTooltipGenerator(t -> ColumnKey.LIMIT.toString());
    }

    protected void initAdditionalReadColumns(ColumnKey columnKey) {
        switch (columnKey) {
            case DRAG_HANDLE -> {
                SerializableConsumer<T> onDown = t -> {
                    if (!editor.isOpen()) {
                        grid.setRowsDraggable(true);
                        draggedItem = t;
                    }
                };

                grid.addColumn(LitRenderer.<T>of(
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

            case STARRED -> {
                Grid.Column<T> starredColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("star",
                                                ("?active=\"${item.starred}\" ")))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .starred(!t.task().starred()))))
                                .withProperty("starred", t ->
                                        t.task().starred()))
                        .setKey(ColumnKey.STARRED.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.STAR))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.STARRED.toString());
            }

            case PINNED -> {
                Grid.Column<T> pinnedColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("pin",
                                                ("?active=\"${item.pinned}\" ")))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .pinned(!t.task().pinned()))))
                                .withProperty("pinned", t ->
                                        t.task().pinned()))
                        .setKey(ColumnKey.PINNED.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.PIN))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.PINNED.toString());
            }

            case CLEANUP -> {
                Grid.Column<T> cleanupColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("recycle",
                                                ("?active=\"${item.cleanup}\" ")))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .cleanup(!t.task().cleanup()))))
                                .withProperty("cleanup", t ->
                                        t.task().cleanup()))
                        .setKey(ColumnKey.CLEANUP.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.RECYCLE))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.CLEANUP.toString());
            }

            case DIFFICULT -> {
                Grid.Column<T> difficultColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("bolt",
                                                ("?active=\"${item.difficult}\" ")))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .difficult(!t.task().difficult()))))
                                .withProperty("difficult", t ->
                                        t.task().difficult()))
                        .setKey(ColumnKey.DIFFICULT.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.BOLT))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.DIFFICULT.toString());
            }

            case EFFORT -> {
//                Grid.Column<T> effortColumn = grid.addComponentColumn(
//                                t -> {
//                                    Integer effort = t.task().effort();
//                                    Select<String> effortSelect = new Select<>();
//                                    effortSelect.setItems(EffortConverter.DEFAULT_EFFORT_STRING, "1", "2", "3", "4", "5");
//                                    effortSelect.setValue(EffortConverter.toPresentation(effort));
//                                    effortSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
//                                    effortSelect.addValueChangeListener(event -> {
//                                        if (event.isFromClient()) {
//                                            controller.requestChangeAsync(new MergeChange<>(
//                                                    new Task(t.task().id())
//                                                            .effort(EffortConverter.toModel(event.getValue())
//                                                                    .getOrThrow(s ->
//                                                                            new IllegalArgumentException("Invalid effort")))));
//                                        }
//                                    });
//                                    return effortSelect;
//                                })
                    Grid.Column<T> effortColumn = grid.addColumn(
                            t -> {
                                Integer effort = t.task().effort();
                                return (effort != null && effort != -1) ? effort : "";
                            })
                            .setKey(ColumnKey.EFFORT.toString())
                            .setHeader(GridUtil.headerIcon(VaadinIcon.TROPHY))
                            .setWidth(GridUtil.ICON_COL_WIDTH_S)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER)
                            .setTooltipGenerator(t -> ColumnKey.EFFORT.toString());
            }

            case REQUIRED -> {
                Grid.Column<T> requiredColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("exclamation-circle-o",
                                                "?active=\"${item.required}\" "))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .required(!t.task().required()))))
                                .withProperty("required", t ->
                                        t.task().required()))
                        .setKey(ColumnKey.REQUIRED.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.EXCLAMATION))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.REQUIRED.toString());

                setMultiEditCheckboxHeader(requiredColumn,
                        t -> t.task().required(),
                        (toggle) ->
                                new Task().required(toggle),
                        t -> t.task().id());
            }

            case PROJECT -> {
                Grid.Column<T> projectColumn = grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("file-tree",
                                                ("?active=\"${item.project}\" ")))
                                .withFunction("onClick", t ->
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .project(!t.task().project()))))
                                .withProperty("project", t ->
                                        t.task().project()))
                        .setKey(ColumnKey.PROJECT.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.FILE_TREE))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.PROJECT.toString());

                setMultiEditCheckboxHeader(projectColumn,
                        t -> t.task().project(),
                        (toggle) ->
                                new Task().project(toggle),
                        t -> t.task().id());
            }

            case TAGS_COMBO -> {
                grid.addColumn(new ComponentRenderer<>(
                                t -> {
                                    TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
                                    tagComboBox.setWidthFull();
                                    tagComboBox.setClassName("grid-combo-box");
                                    tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                                    tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                                    tagComboBox.setValue(taskNetworkGraph.taskTagMap().get(t.task().id()));
                                    tagComboBox.addValueChangeListener(event -> {
                                            if (event.isFromClient()) {
                                                controller.requestChangeAsync(new MergeChange<>(
                                                        new Task(t.task().id())
                                                                .tags(event.getValue())));
                                            }
                                    });
                                    return tagComboBox;
                                }))
                        .setKey(ColumnKey.TAGS_COMBO.toString())
                        .setHeader(ColumnKey.TAGS_COMBO.toString())
                        .setAutoWidth(true)
                        .setFlexGrow(1)
                        .setClassName("tag-column");
            }

            case TAGS -> {
                grid.addColumn(
                                t -> taskNetworkGraph.taskTagMap().get(t.task().id()).stream()
                                        .map(Tag::name)
                                        .collect(Collectors.joining(" | ")))
                        .setKey(ColumnKey.TAGS.toString())
                        .setHeader(ColumnKey.TAGS.toString())
                        .setFlexGrow(1);
            }

            case DURATION -> grid.addColumn(
                            new DurationEstimateValueProvider<>(taskNetworkGraph,
                                    DurationType.TASK_DURATION))
                    .setKey(ColumnKey.DURATION.toString())
                    .setHeader(GridUtil.headerIcon(VaadinIcon.CLOCK))
                    .setWidth(GridUtil.DURATION_COL_WIDTH)
                    .setFlexGrow(0)
                    .setTextAlign(ColumnTextAlign.CENTER)
                    .setTooltipGenerator(t -> ColumnKey.DURATION.toString())
                    .setClassName("duration-column");

            case NET_DURATION -> {
                Button columnHeaderButton = new Button(new Span(
                        GridUtil.headerIconPrimary(VaadinIcon.FILE_TREE_SUB),
                        GridUtil.headerIconPrimary(VaadinIcon.CLOCK)));
                columnHeaderButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

                DurationEstimateValueProvider<T> provider = new DurationEstimateValueProvider<>(taskNetworkGraph,
                        DurationEstimateValueProvider.DurationType.NET_DURATION);
                columnHeaderButton.addSingleClickListener(event -> {
                    log.debug("Toggle net duration format");
                    provider.toggleFormat();
                    grid.getDataProvider().refreshAll();
                });
                columnHeaderButton.addClassName(K.ICON_COLOR_COMPLEMENTARY);

                grid.addColumn(provider)
                        .setKey(ColumnKey.NET_DURATION.toString())
                        .setHeader(columnHeaderButton)
                        .setWidth(GridUtil.DURATION_COL_WIDTH)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.NET_DURATION.toString());
            }
        }
    }

    protected abstract void configureDragAndDrop();

    protected abstract void configureAdditionalEvents();

    @Override
    protected void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        grid.setSelectionMode(selectionMode);
        editHeaderLayout.setVisible(selectionMode.equals(SelectionMode.MULTI));

        this.addMultiMergeTagComboBox();
    }

    protected Collection<Component> configureAdditionalTopBarComponents() {
        return List.of();
    }

    protected Component configureMiddleBar() {
        return null;
    }

    protected void setColumnVisibility(ColumnKey columnKey, boolean visible) {
        visibleColumns.put(columnKey, visible);
    }

    protected boolean getColumnVisibility(ColumnKey columnKey) {
        return visibleColumns.getOrDefault(columnKey, false);
    }

    protected abstract void onManualGridRefresh();

    protected HorizontalLayout gridOptionsMenu(List<ColumnKey> possibleColumns) {
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
            Grid.Column<T> column = grid.getColumnByKey(columnKey.toString());
            if (column != null) {
                column.setVisible(checked);
            } else if (checked) {
                this.initColumn(columnKey);
            }
        };

        MenuItem simpleViewMode = visibilityMenu.getSubMenu().addItem("Simple View");
        MenuItem defaultViewMode = visibilityMenu.getSubMenu().addItem("Default View");
        MenuItem fullViewMode = visibilityMenu.getSubMenu().addItem("Full View");

        visibilityMenu.getSubMenu().add(new Hr());

        Map<ColumnKey, MenuItem> columnVisibilityMap = new HashMap<>();
        possibleColumns.forEach(
                (columnKey) -> {
                    String columnName = columnKey.toString();
                    MenuItem menuItem = visibilityMenu.getSubMenu().addItem(columnName);
                    columnVisibilityMap.put(columnKey, menuItem);
                    menuItem.setCheckable(true);

                    toggleVisibility.accept(columnKey, menuItem, this.getColumnVisibility(columnKey));

                    menuItem.addClickListener(e -> {
                        toggleVisibility.accept(
                                columnKey, menuItem, menuItem.isChecked());
                        if (columnKey.equals(ColumnKey.NET_DURATION)) {
                            NestableTaskNodeTreeFilter filter = SpringContext.getBean(TaskEntryDataProvider.class)
                                        .filter();
                            taskNetworkGraph.getNetDurations(filter);
                        }
                    });
                    // TODO: Use visibleColumns

                    RetainOpenedMenuItemDecorator.keepOpenOnClick(menuItem);
                }
        );

        Set<ColumnKey> simpleViewColumns = Set.of(
                ColumnKey.NAME,
                ColumnKey.DESCRIPTION,
                ColumnKey.FOCUS);

        Consumer<Function<ColumnKey, Boolean>> toggleVisibilitySingle = supplier -> {
            possibleColumns.forEach(columnKey -> {
                boolean visible = supplier.apply(columnKey);
                if (columnVisibilityMap.get(columnKey).isVisible() != visible) {
                    this.setColumnVisibility(columnKey, visible);
                    columnVisibilityMap.get(columnKey).setChecked(visible);
                    Grid.Column<T> column = grid.getColumnByKey(columnKey.toString());
                    if (column != null) {
                        column.setVisible(visible);
                    } else if (visible) {
                        this.initColumn(columnKey);
                    }
                }
            });
        };

        simpleViewMode.addClickListener(e -> toggleVisibilitySingle.accept(
                simpleViewColumns::contains));

        defaultViewMode.addClickListener(e -> toggleVisibilitySingle.accept(
                columnKey -> !settings.excludedFromDefaultViewColumns.contains(columnKey)));

        fullViewMode.addClickListener(e -> toggleVisibilitySingle.accept(
                columnKey -> columnKey != ColumnKey.TAGS));

        MenuItem descriptionsView = subMenu.addItem("Descriptions");

        descriptionsView.getSubMenu().addItem("Expand Existing", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.IF_PRESENT));
        descriptionsView.getSubMenu().addItem("Expand All", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.ALL));
        descriptionsView.getSubMenu().addItem("Hide All", event ->
                settings.descriptionViewDefaultSetting(DescriptionViewDefaultSetting.NONE));

        Button refreshGrid = new Button();
        refreshGrid.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        Icon refreshIcon = VaadinIcon.REFRESH.create();
        refreshIcon.addClassName(K.ICON_COLOR_PRIMARY);
        refreshGrid.setIcon(refreshIcon);
        refreshIcon.setTooltipText("Refresh all data");
        refreshGrid.addClickListener(e -> {
            controller.taskNetworkGraph().reset();
            grid.getDataProvider().refreshAll();
            onManualGridRefresh();
        });

        return new HorizontalLayout(refreshGrid, menuBar);
    }
}
