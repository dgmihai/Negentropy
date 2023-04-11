//package com.trajan.negentropy.vaadin.view;
//
//import com.trajan.negentropy.data.entity.TaskInfo;
//import com.trajan.negentropy.data.entity.TaskRelationship;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.grid.Grid;
//import com.vaadin.flow.component.grid.dnd.GridDropMode;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.textfield.NumberField;
//import com.vaadin.flow.component.textfield.TextArea;
//import com.vaadin.flow.component.textfield.TextField;
//import com.vaadin.flow.component.treegrid.TreeGrid;
//import com.vaadin.flow.router.PageTitle;
//import com.vaadin.flow.router.Route;
//import jakarta.annotation.security.PermitAll;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//
//@Route(value = "", layout = MainLayout.class)
//@PageTitle("Negentropy")
//@PermitAll
//public class TreeView {
//    // Filter toolbar
//    private HorizontalLayout filterLayout;
//    private List<Tag> tags;
//    private Map<Tag, Button> tagButtons;
//
//    // Grid for single TaskInfo entry
//    private Grid<TaskInfo> taskInfoGrid;
//    private TextField titleField;
//    private NumberField priorityField;
//    private TextField etaField;
//    private TextArea descriptionArea;
//
//    // TreeGrid for hierarchy of tasks
//    private TreeGrid<TaskRelationship> taskTreeGrid;
//
//    public TreeView() {
//        // Initialize filter toolbar
//        filterLayout = new HorizontalLayout();
//        tags = new ArrayList<>();
//        tagButtons = new HashMap<>();
//
//        // Initialize grid for single TaskInfo entry
//        taskInfoGrid = new Grid<>();
//        titleField = new TextField();
//        priorityField = new NumberField();
//        etaField = new TextField();
//        descriptionArea = new TextArea();
//
//        // Initialize treegrid for hierarchy of tasks
//        taskTreeGrid = new TreeGrid<>();
//
//        // Configure drag and drop between grids
//        taskInfoGrid.setRowsDraggable(true);
//        taskTreeGrid.setRowsDraggable(true);
//        taskTreeGrid.setDropMode(GridDropMode.BETWEEN);
//
//        // Add components to view
//        add(filterLayout, taskInfoGrid, taskTreeGrid);
//    }
//}
