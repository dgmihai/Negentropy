//package com.trajan.negentropy.client.list;
//
//import com.trajan.negentropy.client.MainLayout;
//import com.trajan.negentropy.client.TaskEntry;
//import com.trajan.negentropy.client.list.util.Insert;
//import com.trajan.negentropy.client.list.util.NestedTaskTabs;
//import com.trajan.negentropy.client.util.TimeButton;
//import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
//import com.trajan.negentropy.server.entity.Tag;
//import com.trajan.negentropy.server.entity.TaskLink;
//import com.vaadin.flow.component.grid.Grid;
//import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
//import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
//import com.vaadin.flow.component.grid.dnd.GridDropMode;
//import com.vaadin.flow.component.html.Hr;
//import com.vaadin.flow.component.icon.Icon;
//import com.vaadin.flow.component.icon.VaadinIcon;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.treegrid.TreeGrid;
//import com.vaadin.flow.router.PageTitle;
//import com.vaadin.flow.router.Route;
//import com.vaadin.flow.router.RouteAlias;
//import lombok.Getter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.StringJoiner;
//
//@Route(value = "", layout = MainLayout.class)
//@PageTitle("Negentropy")
//@RouteAlias("list")
//@Getter
//public class ListView extends VerticalLayout {
//    private static final Logger logger = LoggerFactory.getLogger(ListView.class);
//
//    private final ListViewPresenter presenter;
//
//    private final TreeGrid<TaskEntry> treeGrid;
//    private final TaskTreeContextMenu contextMenu;
//    private final TaskForm taskForm;
//    private final NestedTaskTabs nestedTabs;
//
//    private TaskEntry draggedItem;
//
//    public ListView(@Autowired ListViewPresenter presenter) {
//        this.presenter = presenter;
//
//        this.taskForm = new TaskForm(presenter);
//        this.treeGrid = new TreeGrid<>(TaskEntry.class);
//        this.contextMenu = new TaskTreeContextMenu(this);
//        this.nestedTabs = new NestedTaskTabs(presenter, this);
//
//        addClassName("list-view");
//        setSizeFull();
//
//        presenter.initListView(this);
//
//        initGridColumns();
//        configureEvents();
//        configureDragAndDrop();
//
//        this.add(
//                taskForm,
//                nestedTabs,
//                treeGrid);
//    }
//
//    private void initGridColumns() {
//        Grid.Column<TaskEntry> titleColumn = treeGrid
//                .addHierarchyColumn(entry ->
//                        entry.link().getReferenceTask().getTitle())
//                .setHeader("Title")
//                .setAutoWidth(true)
//                .setFlexGrow(1);
//
//        Grid.Column<TaskEntry> tagColumn = treeGrid
//                .addColumn(entry -> {
//                    Set<Tag> tags = entry.link().getReferenceTask().getTags();
//                    if (tags != null && !tags.isEmpty()) {
//                        StringJoiner joiner = new StringJoiner(", ");
//                        tags.forEach(tag -> joiner.add(tag.getName()));
//                        return joiner.toString();
//                    }
//                    return "";
//                })
//                .setHeader("Tags")
//                .setFlexGrow(1);
//
//        Grid.Column<TaskEntry> descriptionColumn = treeGrid
//                .addColumn(entry ->
//                        entry.link().getReferenceTask().getDescription())
//                .setHeader("Description")
//                .setAutoWidth(true)
//                .setFlexGrow(3);
//
////        Grid.Column<TaskEntry> priorityColumn = treeGrid
////                .addColumn(entry ->
////                        entry.link().getPriority())
////                .setHeader("Priority")
////                .setAutoWidth(true)
////                .setFlexGrow(0);
//
//        TimeButton timeButton = new TimeButton(treeGrid);
//        Grid.Column<TaskEntry> durationColumn = treeGrid
//                .addColumn(new TimeEstimateValueProvider<>(timeButton, presenter.getTaskService()))
//                .setHeader(timeButton)
//                .setFlexGrow(0);
//
//        treeGrid.addComponentColumn(entry -> {
//            if(entry.link().getParentTask() != null) {
//                Icon icon = VaadinIcon.CLOSE_SMALL.create();
//                icon.getElement().getThemeList().add("badge error");
//                icon.addClickListener(e -> presenter.deleteNode(entry.link()));
//                return icon;
//            } else {
//                return null;
//            }})
//                .setFlexGrow(0);
//        treeGrid.setSortableColumns();
//    }
//
//    private void configureEvents() {
//        treeGrid.asSingleSelect().addValueChangeListener(event -> {
//            if (event.getValue() != null) {
//                presenter.setTaskBean(event.getValue().link().getReferenceTask());
//            }
//        });
//
//        treeGrid.addItemDoubleClickListener(e -> {
//            nestedTabs.onSelectNewRootEntry(e.getItem());
//            presenter.clearSelectedTask();
//        });
//    }
//
//    private void configureDragAndDrop() {
//        treeGrid.setRowsDraggable(true);
//        treeGrid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
//
//        treeGrid.addDragStartListener(event -> {
//            logger.debug("Started dragging " + event.getDraggedItems().get(0).link().getReferenceTask().getTitle());
//            this.draggedItem = event.getDraggedItems().get(0);
//        });
//
//        treeGrid.addDropListener(event -> {
//            logger.debug("Dropped onto " + event.getDropTargetItem().orElseThrow().link().getReferenceTask().getTitle());
//            if (event.getDropTargetItem().isPresent()) {
//                TaskLink target = event.getDropTargetItem().get().link();
//                switch (event.getDropLocation()) {
//                    case ABOVE -> {
//                        if (target.getParentTask() != null) {
//                            this.presenter.moveNodeBefore(
//                                    this.draggedItem.link(),
//                                    event.getDropTargetItem().get().link());
//                        } else {
//                            this.presenter.moveNodeAsOrphan(this.draggedItem.link());
//                        }
//                    }
//                    case BELOW -> {
//                        if (target.getParentTask() != null) {
//                            this.presenter.moveNodeAfter(
//                                    this.draggedItem.link(),
//                                    event.getDropTargetItem().get().link());
//                        } else {
//                            this.presenter.moveNodeAsOrphan(this.draggedItem.link());
//                        }
//                    }
//                    case ON_TOP -> this.presenter.moveNodeInto(
//                            this.draggedItem.link(),
//                            event.getDropTargetItem().get().link());
//                }
//            }
//        });
//    }
//
//    private static class TaskTreeContextMenu extends GridContextMenu<TaskEntry> {
//        public TaskTreeContextMenu(ListView listView) {
//            super(listView.treeGrid);
//
//            List<GridMenuItem<TaskEntry>> insertEntries = new ArrayList<>();
//
//            GridMenuItem<TaskEntry> insertBefore = addItem("Insert before", e -> e.getItem().ifPresent(entry ->
//                    listView.presenter.insertTaskNodeFromForm(Insert.BEFORE, entry.link())
//            ));
//
//            GridMenuItem<TaskEntry> insertAfter = addItem("Insert after", e -> e.getItem().ifPresent(entry ->
//                    listView.presenter.insertTaskNodeFromForm(Insert.AFTER, entry.link())
//            ));
//
//            GridMenuItem<TaskEntry> insertAsSubtask = addItem("Insert as subtask", e -> e.getItem().ifPresent(entry ->
//                    listView.presenter.insertTaskNodeFromForm(Insert.AS_SUBTASK_OF, entry.link())
//            ));
//
//            add(new Hr());
//
//            addItem("Remove", e -> e.getItem().ifPresent(entry ->
//                listView.presenter.deleteNode(entry.link())
//            ));
//
//            // Do not show context menu when header is clicked
//            setDynamicContentHandler(entry -> {
//                if (entry == null) return false;
//                insertBefore.setEnabled(listView.presenter.isValid() && entry.link().getParentTask() != null);
//                insertAfter.setEnabled(listView.presenter.isValid() && entry.link().getParentTask() != null);
//                insertAsSubtask.setEnabled(listView.presenter.isValid());
//                return true;
//            });
//        }
//    }
//}
