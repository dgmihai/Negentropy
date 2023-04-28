//package com.trajan.negentropy.client.routine;
//
//
//import com.trajan.negentropy.client.util.TimeButton;
//import com.trajan.negentropy.client.util.TimeEstimateValueProvider;
//import com.trajan.negentropy.server.entity.TagEntity;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.Task_;
//import com.trajan.negentropy.server.repository.filter.Filter;
//import com.trajan.negentropy.server.repository.filter.QueryOperator;
//import com.trajan.negentropy.server.service.TagService;
//import exclude.TaskService;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.checkbox.Checkbox;
//import com.vaadin.flow.component.combobox.MultiSelectComboBox;
//import com.vaadin.flow.component.grid.Grid;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.textfield.TextField;
//import com.vaadin.flow.data.value.ValueChangeMode;
//import lombok.Getter;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.StringJoiner;
//
//@Getter
//public class RoutineSelector extends VerticalLayout {
//    TagService tagService;
//    TaskService taskService;
//
//    private TextField titleFilter = new TextField();
//    private MultiSelectComboBox<TagEntity> tagBox = new MultiSelectComboBox<>();
//    private Grid<Task> taskGrid = new Grid<>();
//    private Button startRoutineButton = new Button("Start Routine");
//    private Checkbox onlyTasksWithETACheckbox = new Checkbox("Only display tasks with a time estimate?");
//
//    public RoutineSelector(TagService tagService, TaskService taskService) {
//        this.tagService = tagService;
//        this.taskService = taskService;
//
//        this.initComponents();
//        this.refreshTaskGrid();
//
//        this.add(
//                titleFilter,
//                tagBox,
//                startRoutineButton,
//                taskGrid);
//    }
//
//    private void initComponents() {
//        titleFilter.setPlaceholder("Filter by Task Name");
//        titleFilter.addValueChangeListener(event -> this.refreshTaskGrid());
//        titleFilter.setValueChangeMode(ValueChangeMode.EAGER);
//        titleFilter.setWidthFull();
//
//        tagBox.setItems(tagService.findAll());
//        tagBox.setPlaceholder("Filter by Tags");
//        tagBox.setItemLabelGenerator(TagEntity::getName);
//        tagBox.addValueChangeListener(event -> this.refreshTaskGrid());
//        tagBox.setWidthFull();
//
//        taskGrid.addColumn(Task::getTitle)
//                .setHeader("Title")
//                .setSortable(true)
//                .setAutoWidth(true);
//
//        taskGrid.addColumn(task -> {
//                    Set<TagEntity> tags = task.getTags();
//                    if (tags != null && !tags.isEmpty()) {
//                        StringJoiner joiner = new StringJoiner(", ");
//                        tags.forEach(tag -> joiner.add(tag.getName()));
//                        return joiner.toString();
//                    }
//                    return "";
//                })
//                .setHeader("Tags")
//                .setSortable(true)
//                .setFlexGrow(1);
//
//        taskGrid.addColumn(Task::getDescription)
//                .setHeader("Description")
//                .setAutoWidth(true)
//                .setFlexGrow(3);
//
//        TimeButton timeButton = new TimeButton(taskGrid);
//        taskGrid.addColumn(new TimeEstimateValueProvider<>(timeButton, taskService))
//                .setHeader(timeButton)
//                .setSortable(true)
//                .setAutoWidth(true);
//
//        taskGrid.addSelectionListener(event -> {
//            startRoutineButton.setEnabled(event.getFirstSelectedItem().isPresent());
//        });
//        startRoutineButton.setEnabled(false);
//        taskGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
//        taskGrid.setWidthFull();
//    }
//
//    private void refreshTaskGrid() {
//        List<Filter> filters = new ArrayList<>();
//        if (!titleFilter.isEmpty()) {
//            filters.add(Filter.builder()
//                    .field(Task_.TITLE)
//                    .operator(QueryOperator.LIKE)
//                    .value(titleFilter.getValue())
//                    .build());
//        }
//        if (onlyTasksWithETACheckbox.getValue()) {
//            filters.add(Filter.builder()
//                    .field(Task_.DURATION)
//                    .operator(QueryOperator.GREATER_THAN)
//                    .value(Duration.ZERO)
//                    .build());
//        }
//
//        Set<TagEntity> tags = tagBox.getValue();
//        List<Task> tasks = tags.isEmpty() ? taskService.findTasks(filters) :
//                taskService.findTasksByTag(filters, tags);
//
//        taskGrid.setItems(tasks);
//    }
//}
