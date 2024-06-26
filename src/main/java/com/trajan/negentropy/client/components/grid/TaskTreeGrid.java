package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.fields.DescriptionTextArea;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.components.grid.subcomponents.NestedTaskTabs;
import com.trajan.negentropy.client.components.grid.subcomponents.RetainOpenedMenuItemDecorator;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.components.taskform.fields.EffortConverter;
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
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.customfield.CustomFieldVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridNoneSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.select.SelectVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.wontlost.ckeditor.VaadinCKEditor;
import lombok.Getter;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Getter
@Benchmark(millisFloor = 10)
public abstract class TaskTreeGrid<T extends HasTaskData> extends Div implements HasRootNode {
    private final UILogger log = new UILogger();

    @Autowired protected UIController controller;
    @Autowired protected TaskNetworkGraph taskNetworkGraph;
    @Autowired protected UserSettings settings;

    protected MultiSelectTreeGrid<T> treeGrid;
    protected Editor<T> editor;
    protected T draggedItem;

    protected NestedTaskTabs nestedTabs;
    protected HorizontalLayout topBar;

    protected LinkedHashMap<ColumnKey, Boolean> visibleColumns;
    protected Grid.Column<T> editColumn;
    protected TagComboBox multiMergeTagComboBox;

    protected FormLayout editHeaderLayout;
    protected List<Checkbox> editCheckboxes;

    protected SelectionMode selectionMode;

    protected abstract MultiSelectTreeGrid<T> createGrid();

    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        log.debug("Init task tree grid");
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
                log.trace("Selection changed");
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
                    Set<T> toUpdate = treeGrid.getSelectedItems();

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
        if (multiMergeTagComboBox == null && !(treeGrid.getSelectionModel() instanceof GridNoneSelectionModel<T>)) {

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
                Set<TaskID> selectedTasks = treeGrid.getSelectedItems()
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

            treeGrid.addSelectionListener(e -> {
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
        } else if (treeGrid.getSelectionModel() instanceof GridNoneSelectionModel<T>
                && multiMergeTagComboBox != null) {
            editHeaderLayout.remove(multiMergeTagComboBox);
            multiMergeTagComboBox = null;
        }
    }

    private void initColumn(ColumnKey columnKey) {
        if (treeGrid.getColumnByKey(columnKey.toString()) == null) {
            switch (columnKey) {
                case DRAG_HANDLE -> {
                    SerializableConsumer<T> onDown = t -> {
                        if (!editor.isOpen()) {
                            treeGrid.setRowsDraggable(true);
                            draggedItem = t;
                        }
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
                        .setResizable(true)
                        .setFrozen(true)
                        .setFlexGrow(1);

                case STARRED -> {
                    Grid.Column<T> starredColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
                    Grid.Column<T> pinnedColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
                    Grid.Column<T> cleanupColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
                    Grid.Column<T> difficultColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
//                    Grid.Column<T> effortColumn = treeGrid.addColumn(
//                            t -> {
//                                Integer effort = t.task().effort();
//                                return (effort != null && effort != -1) ? effort : "";
//                            })
//                            .setKey(ColumnKey.EFFORT.toString())
//                            .setHeader(GridUtil.headerIcon(VaadinIcon.TROPHY))
//                            .setWidth(GridUtil.ICON_COL_WIDTH_S)
//                            .setFlexGrow(0)
//                            .setTextAlign(ColumnTextAlign.CENTER)
//                            .setTooltipGenerator(t -> ColumnKey.EFFORT.toString());


                    Grid.Column<T> effortColumn = treeGrid.addComponentColumn(
                            t -> {
                                Integer effort = t.task().effort();
                                Select<String> effortSelect = new Select<>();
                                effortSelect.setItems(EffortConverter.DEFAULT_EFFORT, "1", "2", "3", "4", "5");
                                effortSelect.setValue(EffortConverter.toPresentation(effort));
                                effortSelect.addThemeVariants(SelectVariant.LUMO_SMALL);
                                effortSelect.addValueChangeListener(event -> {
                                    if (event.isFromClient()) {
                                        controller.requestChangeAsync(new MergeChange<>(
                                                new Task(t.task().id())
                                                        .effort(EffortConverter.toModel(event.getValue())
                                                                .getOrThrow(s ->
                                                                        new IllegalArgumentException("Invalid effort")))));
                                    }
                                });
                                return effortSelect;
                            })
                            .setKey(ColumnKey.EFFORT.toString())
                            .setHeader(GridUtil.headerIcon(VaadinIcon.TROPHY))
                            .setWidth(GridUtil.ICON_COL_WIDTH_S)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER)
                            .setTooltipGenerator(t -> ColumnKey.EFFORT.toString());
                }

                case REQUIRED -> {
                    Grid.Column<T> requiredColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
                    Grid.Column<T> projectColumn = treeGrid.addColumn(LitRenderer.<T>of(
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
                    treeGrid.addColumn(new ComponentRenderer<>(
                                    t -> {
                                        TagComboBox tagComboBox = new CustomValueTagComboBox(controller);
                                        tagComboBox.setWidthFull();
                                        tagComboBox.setClassName("grid-combo-box");
                                        tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                                        tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                                        tagComboBox.setValue(taskNetworkGraph.taskTagMap().get(t.task().id()));
                                        tagComboBox.addValueChangeListener(event ->
                                                controller.requestChangeAsync(new MergeChange<>(
                                                        new Task(t.task().id())
                                                                .tags(event.getValue()))));
                                        return tagComboBox;
                                    }))
                            .setKey(ColumnKey.TAGS_COMBO.toString())
                            .setHeader(ColumnKey.TAGS_COMBO.toString())
                            .setAutoWidth(true)
                            .setFlexGrow(1)
                            .setClassName("tag-column");
                }

                case TAGS -> {
                    treeGrid.addColumn(
                                    t -> taskNetworkGraph.taskTagMap().get(t.task().id()).stream()
                                            .map(Tag::name)
                                            .collect(Collectors.joining(" | ")))
                            .setKey(ColumnKey.TAGS.toString())
                            .setHeader(ColumnKey.TAGS.toString())
                            .setFlexGrow(1);
                }

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
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.DESCRIPTION.toString())
                        .setClassName("description-column");

                case DURATION -> treeGrid.addColumn(
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
                        treeGrid.getDataProvider().refreshAll();
                    });
                    columnHeaderButton.addClassName(K.ICON_COLOR_COMPLEMENTARY);

                    treeGrid.addColumn(provider)
                            .setKey(ColumnKey.NET_DURATION.toString())
                            .setHeader(columnHeaderButton)
                            .setWidth(GridUtil.DURATION_COL_WIDTH)
                            .setFlexGrow(0)
                            .setTextAlign(ColumnTextAlign.CENTER)
                            .setTooltipGenerator(t -> ColumnKey.NET_DURATION.toString());
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
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.EDIT.toString());

                default -> this.initAdditionalReadColumns(columnKey);
            }
        }

        treeGrid.setSortableColumns();
        treeGrid.setColumnReorderingAllowed(true);
        this.setColumnSortOrder();
        this.setPartNameGenerator();
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
        VaadinCKEditor descriptionArea = DescriptionTextArea.inline("Description");
        descriptionArea.addClassName("grayed");
        descriptionArea.addThemeVariants(CustomFieldVariant.LUMO_SMALL);
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
//            descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);

            controller.requestChangeAsync(new MergeChange<>(
                    new Task(t.task().id())
                            .description(descriptionArea.getValue())));
        };

        Runnable cancelEditingDescription = () -> {
            toggleEditing.accept(false);
//            descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);

            descriptionArea.setValue(t.task().description());
        };

        descriptionArea.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                toggleEditing.accept(true);
//                descriptionArea.setValueChangeMode(ValueChangeMode.LAZY);
            }
        });
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

        this.addMultiMergeTagComboBox();
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
            Grid.Column<T> column = treeGrid.getColumnByKey(columnKey.toString());
            if (column != null) {
                column.setVisible(checked);
            } else if (checked) {
                this.initColumn(columnKey);
                this.setColumnSortOrder();
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
                            boolean areNetDurationsVisible = menuItem.isChecked();
                            settings.areNetDurationsVisible(areNetDurationsVisible);
                            if (areNetDurationsVisible) {
                                NestableTaskNodeTreeFilter filter = SpringContext.getBean(TaskEntryDataProvider.class)
                                        .filter();
                                taskNetworkGraph.getNetDurations(filter);
                            }
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
                    Grid.Column<T> column = treeGrid.getColumnByKey(columnKey.toString());
                    if (column != null) {
                        column.setVisible(visible);
                    } else if (visible) {
                        this.initColumn(columnKey);
                    }
                }
            });

            this.setColumnSortOrder();
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
        refreshGrid.addClickListener(e -> {
            treeGrid.getDataProvider().refreshAll();
            onManualGridRefresh();
        });

        return new HorizontalLayout(refreshGrid, menuBar);
    }
}
