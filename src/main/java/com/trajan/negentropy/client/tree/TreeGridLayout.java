package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.tree.util.NestedTaskTabs;
import com.trajan.negentropy.client.util.DurationConverter;
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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Accessors(fluent = true)
@Getter
public class TreeGridLayout extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(TreeGridLayout.class);

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
    private final String DELETE_COL_WIDTH = "30px";

    public TreeGridLayout(TreeViewPresenter presenter) {
        this.presenter = presenter;
        
        this.treeGrid = new TreeGrid<>(TaskEntry.class);
        this.nestedTabs = new NestedTaskTabs(presenter);
        new TaskTreeContextMenu(treeGrid);

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
        // Add browser window listener to observe width change
        getUI().ifPresent(ui -> resizeListener = ui.getPage().addBrowserWindowResizeListener(event -> {
            tagColumn.setVisible(event.getWidth() > BREAKPOINT_PX);
        }));
        // Adjust Grid according to initial width of the screen
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
            VaadinIcon iconWhenTrue, VaadinIcon iconWhenFalse, String color,
            Consumer<TaskEntry> onClick,
            Predicate<TaskEntry> isVisible,
            Predicate<TaskEntry> iconSwap) {
        return new ComponentRenderer<>(entry -> {
            Div div = new Div();
            if (isVisible.test(entry)) {
                Icon iconA = iconWhenTrue.create();
                Icon iconB = iconWhenFalse.create();
                iconA.setSize(ICON_SIZE);
                iconA.setColor(color);
                iconB.setSize(ICON_SIZE);
                iconB.setColor(color);

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
        Editor<TaskEntry> editor = treeGrid.getEditor();

        nameColumn = treeGrid.addHierarchyColumn(
                        entry -> entry.task().name())
                .setHeader("Name")
                .setAutoWidth(true)
                .setFlexGrow(1);

        tagColumn = treeGrid.addComponentColumn(
                entry -> {
                    TagComboBox tagComboBox = new TagComboBox(presenter.tagService());
                    tagComboBox.setReadOnly(true);
                    tagComboBox.setWidthFull();
                    tagComboBox.setClassName("grid-row");
                    tagComboBox.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
                    tagComboBox.setItems(entry.task().tags());
                    return tagComboBox;
                })
                .setHeader("Tags")
                .setAutoWidth(true)
                .setFlexGrow(1);
        tagColumn.setClassName("tag-column");

        Icon descriptionColumnHeaderIcon = VaadinIcon.NOTEBOOK.create();
        descriptionColumnHeaderIcon.setSize(ICON_SIZE);
        descriptionColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.EYE, VaadinIcon.EYE_SLASH, "blue",
                        entry -> {
                    treeGrid.setDetailsVisible(
                            entry,
                            !treeGrid.isDetailsVisible(entry));
                    },
                        entry -> true,
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

        // TODO: Step Duration Column
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

        // TODO: Net Duration Column
        Icon clock = VaadinIcon.CLOCK.create();
        clock.setSize(ICON_SIZE);
        Icon tree = VaadinIcon.FILE_TREE_SUB.create();
        tree.setSize(ICON_SIZE);
        HorizontalLayout netDurationColumnHeader = new HorizontalLayout(tree, clock);
        netDurationColumnHeader.setSpacing(false);
        netDurationColumnHeader.setAlignItems(Alignment.CENTER);
        netDurationColumn = treeGrid.addColumn(
                        new TimeEstimateValueProvider<>(
                                presenter.queryService(),
                                () -> TimeFormat.DURATION,
                                true))
                .setHeader(netDurationColumnHeader)
                .setWidth(DURATION_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        editColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.EDIT, "blue",
                        entry -> {
                    if (editor.isOpen()) {
                        editor.cancel();
                    }
                    editor.editItem(entry);
                    },
                        entry -> true))
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        deleteColumn = treeGrid.addColumn(
                this.inlineButtonRenderer(VaadinIcon.TRASH, "red",
                        presenter::deleteNode,
                        entry -> !presenter.queryService().hasChildren(entry.task().id())
                ))
                .setWidth(DELETE_COL_WIDTH)
                .setFlexGrow(0)
                .setTextAlign(ColumnTextAlign.CENTER);

        treeGrid.setSortableColumns();
    }

    private void initDetails() {
        ComponentRenderer<Component, TaskEntry> detailsRenderer = new ComponentRenderer<>(
                entry -> {
                    TextArea descriptionArea = new TextArea();
                    descriptionArea.setValue(entry.task().description());
                    descriptionArea.setWidthFull();
                    descriptionArea.setReadOnly(true);

                    // TODO: TagComboBox in details on mobile
//                    TagComboBox tagComboBox = new TagComboBox(presenter.tagService());
//                    tagComboBox.setClassName("details");
//                    tagComboBox.setIte

                    Div div = new Div(descriptionArea);

                    Runnable save = () -> {
                        entry.task().description(descriptionArea.getValue());
                        presenter.updateTask(entry);
                        descriptionArea.setReadOnly(true);
                    };

                    div.addClickListener(e -> {
                        descriptionArea.setReadOnly(false);
                    });

                    descriptionArea.addBlurListener(e -> {
                        save.run();
                    });

                    Shortcuts.addShortcutListener(this,
                            save::run,
                            Key.ENTER);

                    Shortcuts.addShortcutListener(this,
                            save::run,
                            Key.ESCAPE);

                    return div;
                });

        treeGrid.setItemDetailsRenderer(detailsRenderer);
        treeGrid.setDetailsVisibleOnClick(false);
    }

    private void initEditColumns() {
        Editor<TaskEntry> editor = treeGrid.getEditor();
        editor.addSaveListener(e -> presenter.updateNode(e.getItem()));

        Binder<TaskEntry> binder = new Binder<>(TaskEntry.class);
        editor.setBinder(binder);
        editor.setBuffered(true);

        // TODO: Standard required messages & localization
        TextField nameField = new TextField();
        nameField.setWidthFull();
        binder.forField(nameField)
                .asRequired("Name must not be empty")
                .bind(
                        entry -> entry.task().name(),
                        (entry, name) -> entry.task().name(name));
        nameColumn.setEditorComponent(nameField);
        //tagColumn.getElement().as(TagComboBox.class).setReadOnly(false);

        TagComboBox tagComboBox = new TagComboBox(presenter.tagService());
        tagComboBox.setWidthFull();
        binder.forField(tagComboBox)
                .bind(
                        entry -> entry.task().tags(),
                        (entry, tags) -> entry.task().tags(tags));
        tagColumn.setEditorComponent(tagComboBox);

        TextField stepDurationField = new TextField();
        stepDurationField.setWidthFull();
        stepDurationField.setTooltipText("Format: 1h 2m 30s");
        stepDurationField.setPattern(DurationConverter.DURATION_PATTERN);
        binder.forField(stepDurationField)
                .withConverter(new DurationConverter())
                .bind(
                        entry -> entry.task().duration(),
                        (entry, duration) -> entry.task().duration(duration));
        stepDurationColumn.setEditorComponent(stepDurationField);

        Label netDurationField = new Label();
        netDurationColumn.setEditorComponent(netDurationField);

        Icon check = VaadinIcon.CHECK.create();
        check.setColor("blue");
        check.addClickListener(e -> editor.save());

        editor.addOpenListener(e ->
                treeGrid.setRowsDraggable(false));
        editor.addCloseListener(e ->
                treeGrid.setRowsDraggable(true));

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
                        if (target.parentId() != null) {
                            presenter.moveNodeBefore(
                                    draggedItem,
                                    event.getDropTargetItem().get());
                        } else {
                            presenter.moveNodeToRoot(draggedItem);
                        }
                    }
                    case BELOW -> {
                        if (target.parentId() != null) {
                            presenter.moveNodeAfter(
                                    draggedItem,
                                    event.getDropTargetItem().get());
                        } else {
                            presenter.moveNodeToRoot(draggedItem);
                        }
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
