package com.trajan.negentropy.client.view;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.client.presenter.ClientPresenter;
import com.trajan.negentropy.client.presenter.Insert;
import com.trajan.negentropy.client.util.NestedTaskTabs;
import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
import com.trajan.negentropy.client.util.ToggleButton;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Negentropy")
@RouteAlias("list")
@Getter
public class ListView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(ListView.class);

    private final ClientPresenter presenter;

    private final TreeGrid<TaskEntry> treeGrid;
    private final TaskTreeContextMenu contextMenu;
    private final TaskInfoForm taskInfoForm;
    private final NestedTaskTabs nestedTabs;

    public ListView(@Autowired ClientPresenter presenter) {
        this.presenter = presenter;

        this.taskInfoForm = new TaskInfoForm(presenter);
        this.treeGrid = new TreeGrid<>(TaskEntry.class);
        this.contextMenu = new TaskTreeContextMenu(this);
        this.nestedTabs = new NestedTaskTabs(presenter, this);

        addClassName("list-view");
        setSizeFull();

        presenter.initListView(this);

        initGridColumns();
        configureEvents();
        configureDragAndDrop();

        add(taskInfoForm,
                nestedTabs,
                treeGrid);
    }

    private void initGridColumns() {
        Grid.Column<TaskEntry> titleColumn = treeGrid
                .addHierarchyColumn(entry ->
                        entry.node().getReferenceTask().getTitle())
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<TaskEntry> descriptionColumn = treeGrid
                .addColumn(entry ->
                        entry.node().getReferenceTask().getDescription())
                .setHeader("Description")
                .setAutoWidth(true)
                .setFlexGrow(3);

        Grid.Column<TaskEntry> priorityColumn = treeGrid
                .addColumn(entry ->
                        entry.node().getReferenceTask().getPriority())
                .setHeader("Priority")
                .setAutoWidth(true)
                .setFlexGrow(0);

        ToggleButton timeButton = new ToggleButton();
        timeButton.setIcon(VaadinIcon.CLOCK.create());
        timeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        timeButton.addClickListener(event -> {
            timeButton.toggle();
            if (timeButton.isToggled()) {
                timeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            } else {
                timeButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            timeButton.setIcon(VaadinIcon.CLOCK.create());
            treeGrid.getDataProvider().refreshAll();
        });
        Grid.Column<TaskEntry> durationColumn = treeGrid
                .addColumn(new TimeEstimateValueProvider(timeButton))
                .setHeader(timeButton)
                .setAutoWidth(true);
        treeGrid.setSortableColumns();
    }



    private void configureEvents() {
        treeGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                presenter.setTaskBean(event.getValue().node().getReferenceTask());
            }
        });

        treeGrid.addItemDoubleClickListener(e -> {
            nestedTabs.onSelectNewRootEntry(e.getItem());
        });



//    public Registration addCloseListener(ComponentEventListener<ClearEvent> listener) {
//        return addListener(ClearEvent.class, listener);
//    }
//        addDataChangeEventListener(this::handleDataChange);
//        taskForm.addSaveListener(e -> {
//        });
//        addDeleteListener(ComponentEventListener < TaskEvent.Save > listener) {
//            return addListener(TaskEvent.Save.class, listener);
//        }
    }



//    public Registration addDataChangeEventListener(ComponentEventListener<ViewEvent> listener) {
//        return addListener(ViewEvent.class, listener);
//    }
//
//    private void handleDataChange(ViewEvent dataChangeEvent) {
//        if (dataChangeEvent instanceof ViewEvent.AddNewTask) {
//            logger.debug("Mo");
//        }
//        switch (dataChangeEvent.getType()) {
//            case ADD -> {
//                if (dataChangeEvent.getData() != null ) {
//                    if (dataChangeEvent.getData() instanceof Task task) {
//                        handleAddNewTask(task);
//                    } else if (dataChangeEvent.getData() instanceof TaskNode node) {
//                        handleAddRelationship();
//                    }
//                }
//            }
//        }
//    }



    private void configureDragAndDrop() {
        DragSource<TaskInfoForm> formDragSource = DragSource.configure(taskInfoForm);
        //formDragSource.setDragData(taskForm.getBinder().getBean());
        taskInfoForm.getBinder().addValueChangeListener(e -> {
            formDragSource.setDraggable(presenter.isValid());
        });

        // create a drop target TreeGrid component
        DragSource<TaskInfoForm> gridDragSource = DragSource.create(taskInfoForm);
        DropTarget<TreeGrid<TaskEntry>> gridDropTarget = DropTarget.create(treeGrid);
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(true);
    }
//        formDragSource.addDragStartListener(event -> {
//            GridDropEvent<Task> gridDropEvent = new GridDropEvent<>(
//                    treeGrid,
//                    true,
//                    new Task(),
//                    GridDropLocation.ON_TOP,
//                    event.);
//        });

//        formDragSource.addDragStartListener(event -> {
//            GridDropEvent<Task> gridDropEvent = new GridDropEvent<>(treeGrid, true, null, "on-top", event.getDragData());
//            formDragSource.setDragData(gridDropEvent);
//        })
//
//        treeGrid.addDropListener(event -> {
////            GridDragStartEvent<Task> gridDragStartEvent = new GridDragStartEvent<>();
////            fireEvent(ComponentEvent< GridDragStartEvent >)
//            logger.debug("Dragging");
//        });
//
////        // register a drop listener to add the Task bean to the grid
//        gridDropTarget.addDropListener( event -> {
//            logger.debug("Dragging");
////            TaskForm taskForm = (TaskForm) event.getDragData().orElse(null);
////            Task task = taskForm.getBinder().getBean();
////            event.getDropTargetRow();
//                });
////            event.getDragData().ifPresent( dataTaskId -> {
////                if (dataTaskId instanceof Task task) {
////                    fireEvent()
////                }
////        });


//            TaskNode dropTarget = e.getDropTargetItem().orElse(null);
//            GridDropLocation dropLocation = e.getDropLocation();
//            Task task = e.getDataTransferData()
//            boolean droppedOntoSelf = draggedTask
//                    .equals(targetTask);
//
//            if (targetTask == null || droppedOntoSelf)
//                return;
//
//
//            if (dropLocation == GridDropLocation.BELOW) {
//                System.out.println("BELOW");
//                //targetTask.addItemAfter(draggedTask, targetTask);
//            } else if (dropLocation == GridDropLocation.ABOVE) {
//                System.out.println("ABOVE");
//                //dataView.addItemBefore(draggedTask, targetTask);
//            } else if (dropLocation == GridDropLocation.ON_TOP) {
//                System.out.println(draggedTask.getName());
//                System.out.println("  " + targetTask.getName());
//                targetTask.getChildren().add(draggedTask);
//                controller.saveTask(targetTask);
//                update();
//            } else if (dropLocation == GridDropLocation.EMPTY) {
//                System.out.println(draggedTask.getName());
//                System.out.println("  " + targetTask.getName());
//                draggedTask.setInstanceParent(null);
//                controller.saveTask(draggedTask);
//                update();
//            }
//        });
//
//
//
//
//
//
//
//
//
//
//
//
//
//            TaskForm sourceComponent = (TaskForm) event.getDragSourceComponent().orElse(null);
//            if (sourceComponent != null) {
//                Task task = (Task) event.getDragData().get();
//                // get the Task bean from the source component
//                TaskNode dropRelationship = TaskNode.builder()
//                        .orderIndex(0)
//                        .task(sourceComponent.getTaskBean())
//                        .parentRelationship(null)
//                        .build();
//                nodeService.save(dropRelationship);
//                update();
//            }
//        });

    private static class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {
        public TaskTreeContextMenu(ListView listView) {
            super(listView.treeGrid);

            List<GridMenuItem<TaskEntry>> insertEntries = new ArrayList<>();

            insertEntries.add(addItem("Insert before", e -> e.getItem().ifPresent(entry ->
                    listView.presenter.insertTaskNode(Insert.BEFORE, entry.node())
            )));

            insertEntries.add(addItem("Insert after", e -> e.getItem().ifPresent(entry ->
                    listView.presenter.insertTaskNode(Insert.AFTER, entry.node())
            )));

            insertEntries.add(addItem("Insert as subtask", e -> e.getItem().ifPresent(entry ->
                    listView.presenter.insertTaskNode(Insert.AS_SUBTASK, entry.node())
            )));

            add(new Hr());

            addItem("Remove", e -> e.getItem().ifPresent(entry ->
                listView.presenter.deleteNode(entry.node())
            ));

            // Do not show context menu when header is clicked
            setDynamicContentHandler(entry -> {
                if (entry == null) return false;
                insertEntries.forEach(menuItem -> menuItem.setEnabled(listView.presenter.isValid()));
                return true;
            });
        }
    }
}
