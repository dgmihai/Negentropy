package com.trajan.negentropy.client.view;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
import com.trajan.negentropy.client.util.ToggleButton;
import com.trajan.negentropy.client.controller.event.ViewEventPublisher;
import com.trajan.negentropy.server.entity.TaskNode;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Negentropy")
@RouteAlias("list")
@Getter
public class ListView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(ListView.class);
    private final ViewEventPublisher viewEventPublisher;

    private final TreeGrid<TaskNode> treeGrid;
    private final TaskTreeContextMenu contextMenu;
    private final TaskInfoForm taskInfoForm;

    private final Label status = new Label("HELLO!");

    public ListView(ViewEventPublisher viewEventPublisher) {
        this.viewEventPublisher = viewEventPublisher;

        this.taskInfoForm = new TaskInfoForm(viewEventPublisher);
        this.treeGrid = new TreeGrid<>(TaskNode.class);
        this.contextMenu = new TaskTreeContextMenu(this);

        addClassName("list-view");
        setSizeFull();

        initGridColumns();
        configureEvents();
        configureDragAndDrop();

        add(    taskInfoForm,
                status,
                treeGrid);

        viewEventPublisher.publishListViewEvent_Update(this, taskInfoForm, new ArrayList<>());
    }

    private void initGridColumns() {
        treeGrid.removeAllColumns();

        Grid.Column<TaskNode> titleColumn = treeGrid
                .addColumn(taskRelationship ->
                    taskRelationship.getChild().getTitle())
                .setHeader("Title")
                .setAutoWidth(true)
                .setFlexGrow(1);

        Grid.Column<TaskNode> descriptionColumn = treeGrid
                .addColumn(taskRelationship ->
                        taskRelationship.getChild().getDescription())
                .setHeader("Description")
                .setAutoWidth(true)
                .setFlexGrow(3);

        Grid.Column<TaskNode> priorityColumn = treeGrid
                .addColumn(taskRelationship ->
                        taskRelationship.getChild().getPriority())
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
        Grid.Column<TaskNode> durationColumn = treeGrid
                .addColumn(new TimeEstimateValueProvider(timeButton))
                .setKey("time")
                .setHeader(timeButton)
                .setAutoWidth(true);
        treeGrid.setSortableColumns();
    }

    private void configureEvents() {
        treeGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                taskInfoForm.setTaskInfoBean(event.getValue().getChild());
            }
        });


//    public Registration addCloseListener(ComponentEventListener<ClearEvent> listener) {
//        return addListener(ClearEvent.class, listener);
//    }
//        addDataChangeEventListener(this::handleDataChange);
//        taskInfoForm.addSaveListener(e -> {
//        });
//        addDeleteListener(ComponentEventListener < TaskInfoEvent.Save > listener) {
//            return addListener(TaskInfoEvent.Save.class, listener);
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
//                    if (dataChangeEvent.getData() instanceof TaskInfo taskInfo) {
//                        handleAddNewTask(taskInfo);
//                    } else if (dataChangeEvent.getData() instanceof TaskRelationship taskRelationship) {
//                        handleAddRelationship();
//                    }
//                }
//            }
//        }
//    }



    private void configureDragAndDrop() {
        DragSource<TaskInfoForm> formDragSource = DragSource.configure(taskInfoForm);
        //formDragSource.setDragData(taskInfoForm.getBinder().getBean());
        taskInfoForm.getBinder().addValueChangeListener(e -> {
            formDragSource.setDraggable(taskInfoForm.getBinder().isValid());
        });

        // create a drop target TreeGrid component
        DragSource<TaskInfoForm> gridDragSource = DragSource.create(taskInfoForm);
        DropTarget<TreeGrid<TaskNode>> gridDropTarget = DropTarget.create(treeGrid);
        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        treeGrid.setRowsDraggable(true);
    }
//        formDragSource.addDragStartListener(event -> {
//            GridDropEvent<TaskInfo> gridDropEvent = new GridDropEvent<>(
//                    treeGrid,
//                    true,
//                    new TaskInfo(),
//                    GridDropLocation.ON_TOP,
//                    event.);
//        });

//        formDragSource.addDragStartListener(event -> {
//            GridDropEvent<TaskInfo> gridDropEvent = new GridDropEvent<>(treeGrid, true, null, "on-top", event.getDragData());
//            formDragSource.setDragData(gridDropEvent);
//        })
//
//        treeGrid.addDropListener(event -> {
////            GridDragStartEvent<TaskInfo> gridDragStartEvent = new GridDragStartEvent<>();
////            fireEvent(ComponentEvent< GridDragStartEvent >)
//            logger.debug("Dragging");
//        });
//
////        // register a drop listener to add the TaskInfo bean to the grid
//        gridDropTarget.addDropListener( event -> {
//            logger.debug("Dragging");
////            TaskInfoForm taskInfoForm = (TaskInfoForm) event.getDragData().orElse(null);
////            TaskInfo taskInfo = taskInfoForm.getBinder().getBean();
////            event.getDropTargetRow();
//                });
////            event.getDragData().ifPresent( data -> {
////                if (data instanceof TaskInfo taskInfo) {
////                    fireEvent()
////                }
////        });


//            TaskRelationship dropTarget = e.getDropTargetItem().orElse(null);
//            GridDropLocation dropLocation = e.getDropLocation();
//            TaskInfo taskInfo = e.getDataTransferData()
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
//            TaskInfoForm sourceComponent = (TaskInfoForm) event.getDragSourceComponent().orElse(null);
//            if (sourceComponent != null) {
//                TaskInfo taskInfo = (TaskInfo) event.getDragData().get();
//                // get the TaskInfo bean from the source component
//                TaskRelationship dropRelationship = TaskRelationship.builder()
//                        .orderIndex(0)
//                        .taskInfo(sourceComponent.getTaskInfoBean())
//                        .parentRelationship(null)
//                        .build();
//                taskRelationshipService.save(dropRelationship);
//                update();
//            }
//        });

    private static class TaskTreeContextMenu extends GridContextMenu<TaskNode> {
        public TaskTreeContextMenu(ListView listView) {
            super(listView.treeGrid);

            List<GridMenuItem<TaskNode>> insertEntries = new ArrayList<>();

            insertEntries.add(addItem("Insert before", e -> e.getItem().ifPresent(taskRelationship -> {
//                listView.viewEventPublisher.publishListViewEvent_Save(listView,
//                        new TaskNode(
//                                taskRelationship.getParent(),
//                                listView.taskInfoForm.getTaskInfoBean(),
//                                taskRelationship.getOrderIndex()));
            })));

            insertEntries.add(addItem("Insert after", e -> e.getItem().ifPresent(taskRelationship -> {
//                listView.viewEventPublisher.publishListViewEvent_Save(listView,
//                        new TaskNode(
//                                taskRelationship.getParent(),
//                                listView.taskInfoForm.getTaskInfoBean(),
//                                taskRelationship.getOrderIndex() + 1));
            })));

            insertEntries.add(addItem("Insert as subtask", e -> e.getItem().ifPresent(taskRelationship -> {
//                listView.viewEventPublisher.publishListViewEvent_Save(listView,
//                        new TaskNode(
//                                taskRelationship,
//                                listView.taskInfoForm.getTaskInfoBean()));
            })));

            add(new Hr());

            addItem("Remove", e -> e.getItem().ifPresent(taskRelationship -> {
                listView.viewEventPublisher.publishListViewEvent_Delete(listView, taskRelationship);
            }));

            // Do not show context menu when header is clicked
            setDynamicContentHandler(taskRelationship -> {
                if (taskRelationship == null) {
                    return false;
                }
                insertEntries.forEach(menuItem -> menuItem.setEnabled(listView.taskInfoForm.getBinder().isValid()));
                return true;
            });
        }
    }
}
