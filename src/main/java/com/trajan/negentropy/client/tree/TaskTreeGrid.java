package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.components.taskform.TaskEntryFormLayout;
import com.trajan.negentropy.client.tree.components.NestedTaskTabs;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.util.TagComboBox;
import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
import com.trajan.negentropy.client.util.TimeFormat;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
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
    private Grid.Column<TaskEntry> stepDurationColumn;
    private Grid.Column<TaskEntry> netDurationColumn;
    private Grid.Column<TaskEntry> editColumn;
    private Grid.Column<TaskEntry> deleteColumn;

    private TaskEntry draggedItem;

    private Registration resizeListener;

    private final String ICON_SIZE = "16px";
    private final int BREAKPOINT_PX = 600;
    private final String DURATION_COL_WIDTH = "80px";
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

        treeGrid.setHeightFull();
        treeGrid.setWidthFull();
        treeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

        this.add(
                nestedTabs,
                treeGrid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        getUI().ifPresent(ui -> resizeListener = ui.getPage().addBrowserWindowResizeListener(event -> {
            tagColumn.setVisible(event.getWidth() > BREAKPOINT_PX);
        }));

        getUI().ifPresent(ui -> ui.getPage().retrieveExtendedClientDetails(receiver -> {
            int browserWidth = receiver.getBodyClientWidth();
            tagColumn.setVisible(browserWidth > BREAKPOINT_PX);
        }));
    }

    private ComponentRenderer<Component, TaskEntry> inlineButtonRenderer(VaadinIcon icon, String color) {
        return new ComponentRenderer<>(entry -> {
            Icon i = icon.create();
            i.setSize(ICON_SIZE);
            i.setColor(color);
            return i;
        });
    }

    private Component inlineButton(VaadinIcon icon, String color, Runnable onClick) {
            Div div = new Div();
            Icon i = icon.create();
            i.setSize(ICON_SIZE);
            i.setColor(color);
            i.addClickListener(e -> onClick.run());
            div.add(i);
            return div;
    }

    private ComponentRenderer<Component, TaskEntry> inlineButtonRenderer(
            VaadinIcon icon, String color, Consumer<TaskEntry> onClick, Predicate<TaskEntry> isVisible) {
        return new ComponentRenderer<>(entry -> {
            Div div = new Div();
            if(isVisible.test(entry)) {
                Icon i = icon.create();
                i.setSize(ICON_SIZE);
                i.setColor(color);
                i.addClickListener(e -> onClick.accept(entry));
                div.add(i);
            }
            return div;
        });
    }

    private ComponentRenderer<Component, TaskEntry> inlineButtonRenderer(
            VaadinIcon iconWhenTrue, VaadinIcon iconWhenFalse,
            Consumer<TaskEntry> onClick,
            Predicate<TaskEntry> isVisible,
            Predicate<TaskEntry> iconSwap) {
        return new ComponentRenderer<>(entry -> {
            Div div = new Div();
            if (isVisible.test(entry)) {
                Icon iconA = iconWhenTrue.create();
                Icon iconB = iconWhenFalse.create();
                iconA.setSize(ICON_SIZE);
                iconA.setColor(LumoUtility.TextColor.PRIMARY_CONTRAST);
                iconB.setSize(ICON_SIZE);
                iconB.setColor(LumoUtility.TextColor.PRIMARY_CONTRAST);

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
                .setHeader("Name")
                .setAutoWidth(true)
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
                .setHeader("Tags")
                .setAutoWidth(true)
                .setFlexGrow(1);
        tagColumn.setClassName("tag-column");

        Icon descriptionColumnHeaderIcon = VaadinIcon.NOTEBOOK.create();
        descriptionColumnHeaderIcon.setSize(ICON_SIZE);
        descriptionColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.EYE, VaadinIcon.EYE_SLASH,
                        entry -> {
                    treeGrid.setDetailsVisible(
                            entry,
                            !treeGrid.isDetailsVisible(entry));
                    },
                        entry -> !entry.task().description().isBlank(),
                        treeGrid::isDetailsVisible))
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

        Icon stepDurationColumnHeaderIcon = VaadinIcon.CLOCK.create();
        stepDurationColumnHeaderIcon.setSize(ICON_SIZE);
        stepDurationColumn = treeGrid.addColumn(
                        new TimeEstimateValueProvider<>(
                                presenter.queryService(),
                                () -> TimeFormat.DURATION,
                                false))
                .setHeader(stepDurationColumnHeaderIcon)
                .setWidth(DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        Icon clock = VaadinIcon.CLOCK.create();
        clock.setSize(ICON_SIZE);
        Icon tree = VaadinIcon.FILE_TREE_SUB.create();
        tree.setSize(ICON_SIZE);
        HorizontalLayout netDurationColumnHeader = new HorizontalLayout(tree, clock);
        netDurationColumnHeader.setSpacing(false);
        netDurationColumnHeader.setJustifyContentMode(JustifyContentMode.CENTER);
        netDurationColumnHeader.setWidth(DURATION_COL_WIDTH);
        netDurationColumn = treeGrid.addColumn(
                        new TimeEstimateValueProvider<>(
                                presenter.queryService(),
                                () -> TimeFormat.DURATION,
                                true))
                .setHeader(netDurationColumnHeader)
                .setWidth(DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        editColumn = treeGrid.addColumn(this.inlineButtonRenderer(
                    VaadinIcon.EDIT,
                    LumoUtility.TextColor.PRIMARY_CONTRAST,
                    entry -> {
                        if (editor.isOpen()) {
                            editor.cancel();
                        }
                        editor.editItem(entry);
                        treeGrid.setDetailsVisible(entry, true);
                    },
                    entry -> true))
                .setWidth(ICON_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        deleteColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(
                        VaadinIcon.TRASH,
                        "red",
                        presenter::deleteNode,
                        entry -> !presenter.queryService().hasChildren(entry.task().id(), null)
                ))
                .setWidth(ICON_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        editColumn.setHeader(inlineButton(VaadinIcon.TRASH, "red",
                () -> deleteColumn.setVisible(!deleteColumn().isVisible())));
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
        check.setColor("blue");
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
}
