package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.taskform.TaskEntryFormLayout;
import com.trajan.negentropy.client.tree.components.InlineIconButton;
import com.trajan.negentropy.client.tree.components.NestedTaskTabs;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.util.TagComboBox;
import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(fluent = true)
@Getter
public class TaskTreeGrid extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(TaskTreeGrid.class);

    private final TreeViewPresenter presenter;

    private final TreeGrid<TaskEntry> treeGrid;
    private final NestedTaskTabs nestedTabs;

    private Grid.Column<TaskEntry> nameColumn;
    private Grid.Column<TaskEntry> tagColumn;
    private Grid.Column<TaskEntry> descriptionColumn;
    private Grid.Column<TaskEntry> taskDurationColumn;
    private Grid.Column<TaskEntry> timeEstimateColumn;
    private Grid.Column<TaskEntry> editColumn;
    private Grid.Column<TaskEntry> deleteColumn;

    private TaskEntry draggedItem;

    private final String DURATION_COL_WIDTH = "50px";
    private final String ICON_COL_WIDTH = "30px";

    private Editor<TaskEntry> editor;
    private TaskEntryFormLayout form;

    public TaskTreeGrid(TreeViewPresenter presenter) {
        this.presenter = presenter;
        
        this.treeGrid = new TreeGrid<>(TaskEntry.class);
        this.nestedTabs = new NestedTaskTabs(presenter);
        new TaskTreeContextMenu(treeGrid);
        this.editor = treeGrid.getEditor();

        initReadColumns();
        initEditColumns();
        initDetails();
        configureDragAndDrop();
        configureSelection();

        nestedTabs.addThemeVariants(TabsVariant.LUMO_SMALL);

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        treeGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);

        this.setPadding(false);
        this.add(
                nestedTabs,
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
        nameColumn = treeGrid.addHierarchyColumn(
                        entry -> entry.task().name())
                .setKey("name")
                .setHeader("Name")
                .setWidth("150px")
                .setFrozen(true)
                .setFlexGrow(1);

        tagColumn = treeGrid.addComponentColumn(
                entry -> {
                    TagComboBox tagComboBox = new TagComboBox(presenter);
                    tagComboBox.setReadOnly(true);
                    tagComboBox.setWidthFull();
                    tagComboBox.setClassName("grid-row");
                    tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                    tagComboBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
                    tagComboBox.setValue(entry.task().tags());
                    return tagComboBox;
                })
                .setKey("tags")
                .setHeader("Tags")
                .setAutoWidth(true)
                .setFlexGrow(1);
        tagColumn.setClassName("tag-column");

        Icon descriptionColumnHeaderIcon = VaadinIcon.NOTEBOOK.create();
        descriptionColumnHeaderIcon.setSize(K.INLINE_ICON_SIZE);
        descriptionColumnHeaderIcon.addClassName("unselected-color-icon");
        descriptionColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.EYE, VaadinIcon.EYE_SLASH,
                        entry -> {
                            treeGrid.setDetailsVisible(
                                    entry,
                                    !treeGrid.isDetailsVisible(entry));
                        },
                        entry -> !entry.task().description().isBlank(),
                        treeGrid::isDetailsVisible))
                .setKey("description")
                .setHeader(descriptionColumnHeaderIcon)
                .setAutoWidth(true)
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
                .setKey("duration")
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
                .setKey("time estimate")
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
                .setKey("edit")
                .setWidth(ICON_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        deleteColumn = treeGrid.addColumn(
                new ComponentRenderer<Component, TaskEntry>(entry -> {
                    Div div = new Div();
                    if(!entry.hasChildren()) {
                        InlineIconButton iconButton = new InlineIconButton(VaadinIcon.TRASH.create());
                        iconButton.getIcon().addClassName("error-color-icon");
                        iconButton.addClickListener(event -> presenter.deleteNode(entry));
                        div.add(iconButton);
                    }
                    return div;
                }))
                .setKey("delete")
                .setWidth(ICON_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);


        InlineIconButton trashIconButton = new InlineIconButton(VaadinIcon.TRASH.create());
        trashIconButton.getIcon().addClassName("error-color-icon");
        trashIconButton.addClickListener(event -> deleteColumn.setVisible(!deleteColumn().isVisible()));
        editColumn.setHeader(trashIconButton);
        deleteColumn.setVisible(false);

        treeGrid.setSortableColumns();
    }

    private void initDetails() {
        ComponentRenderer<Component, TaskEntry> detailsRenderer = new ComponentRenderer<>(entry -> {
            Predicate<TaskEntry> isBeingEdited = ntry -> ntry.equals(editor.getItem());

            Div detailsDiv = new Div();

            if (isBeingEdited.test(entry)) {
                TaskEntryFormLayout form = new TaskEntryFormLayout(presenter, entry);
                form.addClassNames(LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Padding.Vertical.NONE,
                        LumoUtility.BoxSizing.BORDER);
                form.onClear(() -> editor.closeEditor());
                form.onSave(() -> editor.save());

                editor.setBinder(form.binder());

                detailsDiv.add(form);
            } else if (!entry.task().description().isBlank()) {
                TextArea descriptionArea = new TextArea();
                descriptionArea.setValue(entry.task().description());
                descriptionArea.setSizeUndefined();
                descriptionArea.setWidthFull();
                descriptionArea.setReadOnly(true);

                detailsDiv.add(descriptionArea);
//
//                Runnable saveDescription = () -> {
//                    logger.debug("Saving description from details");
//                    entry.task().description(descriptionArea.getValue());
//                    presenter.updateTask(entry);
//                    descriptionArea.setReadOnly(true);
//                };
//
//                detailsDiv.addClickListener(e -> {
//                    descriptionArea.setReadOnly(false);
//                });
//
//                descriptionArea.addBlurListener(e -> {
//                    saveDescription.run();
//                });
//
//                Shortcuts.addShortcutListener(detailsDiv,
//                        saveDescription::run,
//                        Key.ENTER);
//
//                Shortcuts.addShortcutListener(detailsDiv,
//                        saveDescription::run,
//                        Key.ESCAPE);
            }
            return detailsDiv;
        });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
        treeGrid.setDetailsVisibleOnClick(false);
    }

    private void initEditColumns() {
        Editor<TaskEntry> editor = treeGrid.getEditor();
        editor.addSaveListener(e -> presenter.updateTask(e.getItem()));

        editor.setBuffered(true);

        Icon check = VaadinIcon.CHECK.create();
        check.addClassNames("primary-color-icon");
        check.addClickListener(e -> editor.save());

        AtomicReference<Optional<Registration>> enterListener = new AtomicReference<>(Optional.empty());
        AtomicReference<Optional<Registration>> escapeListener = new AtomicReference<>(Optional.empty());

        editor.addOpenListener(e -> {
            escapeListener.get().ifPresent(Registration::remove);
            treeGrid.setRowsDraggable(false);
            enterListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
                    editor::save,
                    Key.ENTER)));
        });

        editor.addCloseListener(e -> {
            enterListener.get().ifPresent(Registration::remove);
            treeGrid.setRowsDraggable(true);
            treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
            escapeListener.set(Optional.of(Shortcuts.addShortcutListener(treeGrid,
                    editor::cancel,
                    Key.ESCAPE)));
        });

        editColumn.setEditorComponent(check);
    }

    private void configureDragAndDrop() {
        treeGrid.setRowsDraggable(true);
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);

        treeGrid.addDragStartListener(event -> {
            logger.debug("Started dragging " + event.getDraggedItems().get(0).task().name());
            draggedItem = event.getDraggedItems().get(0);
        });

        treeGrid.addDropListener(event -> {
            logger.debug("Dropped onto " + event.getDropTargetItem().orElseThrow().task().name());
            if (event.getDropTargetItem().isPresent()) {
                TaskNode target = event.getDropTargetItem().get().node();
                switch (event.getDropLocation()) {
                    case ABOVE -> {
                        presenter.moveNodeBefore(
                                draggedItem,
                                event.getDropTargetItem().get());
                    }
                    case BELOW -> {
                        presenter.moveNodeAfter(
                                draggedItem,
                                event.getDropTargetItem().get());
                    }
                    case ON_TOP -> presenter.moveNodeInto(
                            draggedItem,
                            event.getDropTargetItem().get());
                }
            }
        });
    }
    
    private void configureSelection() {
        // TODO: Selection mode versatility
        treeGrid.setSelectionMode(Grid.SelectionMode.NONE);

        treeGrid.addItemDoubleClickListener(e -> {
            if (e.getItem() != null) {
                nestedTabs.onSelectNewRootEntry(e.getItem());
            }
        });
    }

    private class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {

        public TaskTreeContextMenu(TreeGrid<TaskEntry> grid) {
            super(grid);

            GridMenuItem<TaskEntry> insertBefore = addItem("Insert before", e -> e.getItem().ifPresent(
                    presenter::addTaskFromFormBefore
            ));

            GridMenuItem<TaskEntry> insertAfter = addItem("Insert after", e -> e.getItem().ifPresent(
                    presenter::addTaskFromFormAfter
            ));

            GridMenuItem<TaskEntry> insertAsSubtask = addItem("Insert as subtask", e -> e.getItem().ifPresent(
                    presenter::addTaskFromFormAsChild
            ));

            add(new Hr());

            addItem("Remove", e -> e.getItem().ifPresent(
                    presenter::deleteNode
            ));

            // Do not show context menu when header  is clicked
            setDynamicContentHandler(entry -> {
                if (entry == null) {
                    return false;
                } else {
                    insertBefore.setEnabled(presenter.isTaskFormValid() && entry.node().parentId() != null);
                    insertAfter.setEnabled(presenter.isTaskFormValid() && entry.node().parentId() != null);
                    insertAsSubtask.setEnabled(presenter.isTaskFormValid());
                    return true;
                }
            });
        }
    }

    public Button visibilityMenu() {
        Button menuButton = new Button("Column", VaadinIcon.EYE.create());
        menuButton.setIconAfterText(false);
        ColumnVisibilityToggleMenu columnVisibilityToggleMenu = new ColumnVisibilityToggleMenu(
                menuButton);
        columnVisibilityToggleMenu.addColumnToggleItem("Tags",
                tagColumn);
        columnVisibilityToggleMenu.addColumnToggleItem("Description",
                descriptionColumn);
        columnVisibilityToggleMenu.addColumnToggleItem("Task Duration",
                taskDurationColumn);
        columnVisibilityToggleMenu.addColumnToggleItem("Total Time Estimate",
                timeEstimateColumn);
        columnVisibilityToggleMenu.addColumnToggleItem("Edit",
                editColumn);

        return menuButton;
    }

    private static class ColumnVisibilityToggleMenu extends ContextMenu {
        public ColumnVisibilityToggleMenu(Component target) {
            super(target);
            this.setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<TaskEntry> column) {
            MenuItem menuItem = this.addItem(label, e ->
                    column.setVisible(e.getSource().isChecked()));
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
    }
}
