package com.trajan.negentropy.client.components.grid;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.fields.DescriptionTextArea;
import com.trajan.negentropy.client.components.grid.enums.ColumnKey;
import com.trajan.negentropy.client.components.grid.subcomponents.InlineIconButton;
import com.trajan.negentropy.client.components.taskform.AbstractTaskFormLayout;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.data.HasTaskData;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomFieldVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.wontlost.ckeditor.VaadinCKEditor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@SpringComponent
@RouteScope
@Getter
@Benchmark(millisFloor = 10)
public abstract class AbstractTaskGrid <T extends HasTaskData> extends VerticalLayout {
    private final UILogger log = new UILogger();

    @Autowired protected UIController controller;

    protected Editor<T> editor;
    protected Grid<T> grid;

    protected LinkedHashMap<ColumnKey, Boolean> visibleColumns;
    protected Grid.Column<T> editColumn;
    protected FormLayout editHeaderLayout;
    protected List<Checkbox> editCheckboxes;

    public void init(LinkedHashMap<ColumnKey, Boolean> visibleColumns, SelectionMode selectionMode) {
        log.debug("Init task grid");
        grid = createGrid();
        this.visibleColumns = visibleColumns;

        editor = grid.getEditor();

        editHeaderLayout = new FormLayout();
        editCheckboxes = new LinkedList<>();

        setSelectionMode(selectionMode);
        initReadColumns();
        initEditColumns();
        initDetails();

        this.setSpacing(false);
        this.setPadding(false);
        grid.setHeightFull();
        grid.setWidthFull();
        grid.addThemeVariants(
                GridVariant.LUMO_COMPACT,
                GridVariant.LUMO_WRAP_CELL_CONTENT);

        addComponents();
    }

    protected void addComponents() {
        add(grid);
    }

    protected abstract Grid<T> createGrid();

    protected abstract void setSelectionMode(SelectionMode selectionMode);

    protected void initReadColumns() {
        visibleColumns.forEach((columnKey, visible) -> {
            if (visible != null && visible) {
                initColumn(columnKey);
            }
        });
    }

    protected Grid.Column<T> addEditorColumn() {
        return grid.addColumn(LitRenderer.<T>of(
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
    }

    protected void initColumn(ColumnKey columnKey) {
        if (grid.getColumnByKey(columnKey.toString()) == null) {
            switch (columnKey) {
                case NAME -> {
                    Grid.Column<T> nameColumn;
                    if (grid instanceof TreeGrid<T> treeGrid) {
                        nameColumn = treeGrid.addHierarchyColumn(
                                        t -> t.task().name());
                    } else {
                        nameColumn = grid.addColumn(
                                        t -> t.task().name());
                    }
                    nameColumn
                            .setKey(ColumnKey.NAME.toString())
                            .setHeader(ColumnKey.NAME.toString())
                            .setWidth("150px")
                            .setResizable(true)
                            .setFrozen(true)
                            .setFlexGrow(1);
                }

                case DESCRIPTION -> grid.addColumn(LitRenderer.<T>of(
                                        GridUtil.inlineVaadinIconLitExpression("clipboard-text",
                                                "?active=\"${item.hasDescription}\""))
                                .withFunction("onClick", t ->
                                        grid.setDetailsVisible(
                                                t,
                                                !grid.isDetailsVisible(t)))
                                .withProperty("hasDescription", t ->
                                        !t.task().description().isBlank()))
                        .setKey(ColumnKey.DESCRIPTION.toString())
                        .setHeader(GridUtil.headerIcon(VaadinIcon.CLIPBOARD_TEXT))
                        .setWidth(GridUtil.ICON_COL_WIDTH_L)
                        .setFlexGrow(0)
                        .setTextAlign(ColumnTextAlign.CENTER)
                        .setTooltipGenerator(t -> ColumnKey.DESCRIPTION.toString())
                        .setClassName("description-column");

                case EDIT -> editColumn = addEditorColumn();

                default -> this.initAdditionalReadColumns(columnKey);
            }
        }

        grid.setSortableColumns();
        grid.setColumnReorderingAllowed(true);
        this.setColumnSortOrder();
        this.setPartNameGenerator();
    }

    protected abstract void initAdditionalReadColumns(ColumnKey columnKey);

    protected abstract void setPartNameGenerator();

    protected abstract List<ColumnKey> getPossibleColumns();

    private void setColumnSortOrder() {
        List<String> columns = grid.getColumns().stream()
                .map(Grid.Column::getKey)
                .toList();
        List<Grid.Column<T>> columnOrder = new LinkedList<>();

        getPossibleColumns().forEach(column -> {
            if(columns.contains(column.toString())) {
                columnOrder.add(grid.getColumnByKey(column.toString()));
            }
        });

        grid.setColumnOrder(columnOrder);
    }

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
                () -> SessionServices.ifNotMobile(saveDescription),
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

        grid.setItemDetailsRenderer(detailsRenderer);
        grid.setDetailsVisibleOnClick(false);
    }

    protected void initEditColumns() {
        if (visibleColumns.getOrDefault(ColumnKey.EDIT, false)) {
            Editor<T> editor = grid.getEditor();
            this.setEditorSaveListener();

            editor.setBuffered(true);

            InlineIconButton check = new InlineIconButton(VaadinIcon.CHECK.create());
            check.addClickListener(e -> {
                grid.setDetailsVisible(editor.getItem(), false);
                editor.save();
            });

            AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
            AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());

            editor.addOpenListener(e -> {
                grid.setDetailsVisible(e.getItem(), true);
                escapeListener.get().ifPresent(Registration::remove);
            });

            editor.addCloseListener(e -> {
                grid.setDetailsVisible(e.getItem(), false);
                enterListener.get().ifPresent(Registration::remove);
                grid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
                escapeListener.set(Optional.of(Shortcuts.addShortcutListener(grid,
                        editor::cancel,
                        Key.ESCAPE)));
                if (e.getItem().task().description().isBlank()) {
                    grid.setDetailsVisible(e.getItem(), false);
                }
            });

            editColumn.setEditorComponent(check);
        }
    }

    protected abstract Registration setEditorSaveListener();
}
