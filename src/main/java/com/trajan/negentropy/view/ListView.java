package com.trajan.negentropy.view;

import com.trajan.negentropy.controller.ViewController;
import com.trajan.negentropy.data.entity.Task;
import com.trajan.negentropy.view.util.TimeEstimateValueProvider;
import com.trajan.negentropy.view.util.ToggleButton;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.BrowserWindowResizeListener;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Scope("prototype")
@Route(value = "", layout = MainLayout.class)
@RouteAlias("list")
@PageTitle("Negentropy")
@PermitAll
public class ListView extends VerticalLayout {
    private final TreeGrid<Task> grid;
    public TextField filterText = new TextField();
    private TaskForm form;
    private Task draggedTask;
    private FlexLayout content;
    private final ViewController controller;

    @Autowired
    public ListView(ViewController controller) {
        this.form = new TaskForm(controller);
        this.controller = controller;

        addClassName("list-view");
        setSizeFull();
        grid = new TreeGrid<>();
        configureGrid();
        configureForm();
        configureResize();
        add(getToolbar(), (Component) getContent());
        update();
    }

    private FlexLayout getContent() {
        content = new FlexLayout(grid, form);
//        content.setFlexGrow(2, grid);
//        content.setFlexGrow(1, form);
        content.addClassNames("content");
        form.getStyle().set("padding", "0 0 0 10px" );
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.addHierarchyColumn(Task::getName)
                .setKey("name")
                .setHeader("Name")
                .setSortable(false)
                .setTextAlign(ColumnTextAlign.CENTER)
                .setAutoWidth(true);
        grid.addColumn(Task::getDescription)
                .setKey("description")
                .setHeader("Description")
                .setAutoWidth(true);
//        grid.addColumn(
//                new ComponentRenderer<>((ValueProvider<Task, Icon>) task -> {
//                    if (task.isPassive()) {
//                        Icon icon = VaadinIcon.CHECK.create();
//                        icon.setColor("gray");
//                        icon.setSize("10px");
//                        return icon;
//                    } else {
//                        return null;
//                    }
//                }))
//                .setKey("passive")
//                .setHeader("Passive")
//                .setAutoWidth(true)
//                .setFlexGrow(0)
//                .setTextAlign(ColumnTextAlign.CENTER);

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
            grid.getDataProvider().refreshAll();
        });

        grid.addColumn(new TimeEstimateValueProvider(timeButton))
                .setKey("time")
                .setHeader(timeButton)
                .setAutoWidth(true);

        grid.addColumn(
            new ComponentRenderer<>(Button::new, (button, task) -> {
                button.addClickListener(e -> {
                    if(form.parent.getValue() != task) {
                        form.parent.setValue(task);
                    }
                });
                Icon icon = new Icon(VaadinIcon.PLUS);
                //icon.setSize("20px");
                button.addThemeVariants(ButtonVariant.LUMO_SMALL);
                button.setIcon(icon);
            }))
                .setKey("edit")
                .setFrozenToEnd(true)
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addClassNames("task-grid");
        grid.setSizeFull();

        grid.asSingleSelect().addValueChangeListener(event ->
                editTask(event.getValue()));

        grid.setRowsDraggable(true);
        grid.setDropMode(GridDropMode.ON_TOP_OR_BETWEEN);
        grid.addDragStartListener(e ->
                draggedTask = e.getDraggedItems().get(0));
        grid.addDropListener(e -> {
            Task targetTask = e.getDropTargetItem().orElse(null);
            GridDropLocation dropLocation = e.getDropLocation();
            boolean droppedOntoSelf = draggedTask
                    .equals(targetTask);

            if (targetTask == null || droppedOntoSelf)
                return;


            if (dropLocation == GridDropLocation.BELOW) {
                System.out.println("BELOW");
                //targetTask.addItemAfter(draggedTask, targetTask);
            } else if (dropLocation == GridDropLocation.ABOVE) {
                System.out.println("ABOVE");
                //dataView.addItemBefore(draggedTask, targetTask);
            } else if (dropLocation == GridDropLocation.ON_TOP) {
                System.out.println(draggedTask.getName());
                System.out.println("  " + targetTask.getName());
                targetTask.getChildren().add(draggedTask);
                controller.saveTask(targetTask);
                update();
            } else if (dropLocation == GridDropLocation.EMPTY) {
                System.out.println(draggedTask.getName());
                System.out.println("  " + targetTask.getName());
                draggedTask.newParent(null);
                controller.saveTask(draggedTask);
                update();
            }
        });

        try (ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor()) {
            // Calculate the initial delay until the next minute
            long initialDelay = 60 - LocalTime.now().getSecond();

            executorService.scheduleAtFixedRate(() -> {
                grid.getDataProvider().refreshAll();
            }, initialDelay, 60, TimeUnit.SECONDS);
        }
    }

    private void configureForm() {
        form.setWidth("30em");
        form.addSaveListener(this::saveTask);
        form.addDeleteListener(this::deleteTask);
        form.addCloseListener(e -> form.clear());
        form.clear();
    }

    private void saveTask(TaskForm.SaveEvent event) {
        saveTask(event.getTask());
    }

    private void saveTask(Task task) {
        controller.saveTask(task);
        update();
        form.clear();
    }

    private void deleteTask(TaskForm.DeleteEvent event) {
        deleteTask(event.getTask());
    }

    private void deleteTask(Task task) {
        controller.getService().deleteTask(task);
        update();
        form.clear();
    }

    private void configureResize() {
        List<String> columnKeys = Arrays.asList(
                "description",
                "passive");
        List<TreeGrid.Column<Task>> columns = new ArrayList<>();
        columnKeys.forEach( key -> {
            columns.add(grid.getColumnByKey(key));
        });

        BrowserWindowResizeListener listener = event -> {
            if (event.getWidth() < 600) {
                columns.forEach( column -> column.setVisible(false));
                content.setFlexDirection(FlexLayout.FlexDirection.COLUMN_REVERSE);
            } else {
                columns.forEach( column -> column.setVisible(true));
                content.setFlexDirection(FlexLayout.FlexDirection.ROW);
            }
        };

        UI.getCurrent().getPage().addBrowserWindowResizeListener(listener);
    }

    private com.vaadin.flow.component.Component getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.EAGER);
        filterText.addValueChangeListener(e -> {
            controller.setFilterText(filterText.getValue());
            update();
        });

        Button addTaskButton = new Button("New Task");
        addTaskButton.addClickListener(click -> addTask());

        var toolbar = new HorizontalLayout(filterText, addTaskButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    public void editTask(Task task) {
        if (task == null) {
            form.clear();
        } else {
            form.setTask(task);
        }
    }

    private void addTask() {
        grid.asSingleSelect().clear();
        form.clear();
    }

    private void update() {
        grid.setItems(controller.findRootTasks(), Task::getChildren);
    }

    private void expandRecursively(Task task) {
//        if (task.getParent() != null) {
//            if (task.getParent().equals(task)) {
//                System.out.println("AFUCK?");
//            } else {
//                System.out.println(task.getName());
//                grid.expand(task);
//                expandRecursively(task.getParent());
//            }
//        }
    }
}