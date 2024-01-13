package com.trajan.negentropy.client;

import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Record;
import com.trajan.negentropy.model.RecordSpan;
import com.trajan.negentropy.model.RecordSpan.RecordSpanEntry;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;


@PageTitle("Records")
@Route(value = "records", layout = MainLayout.class)
@Uses(Icon.class)
@RouteScope
public class RecordView extends VerticalLayout {
    private final UILogger log = new UILogger();

    @Autowired private SessionServices services;
    @Autowired private TaskNetworkGraph taskNetworkGraph;

    @PostConstruct
    public void init() {
        this.addClassName("record-view");
        this.setSizeFull();

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPlaceholder("From");
        startDatePicker.setClearButtonVisible(true);
        startDatePicker.setValue(LocalDate.now().minusDays(7));
        startDatePicker.setWidth("150px");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPlaceholder("To");
        endDatePicker.setClearButtonVisible(true);
        endDatePicker.setValue(LocalDate.now());
        endDatePicker.setWidth("150px");

        Button getAllButton = new Button(VaadinIcon.CHECK.create());

        Tab chronological = new Tab("Chronological");
        Tab byTask = new Tab("By Task");
        Tab byTree = new Tab("As Tree");
        TabSheet tabs = new TabSheet();
        tabs.setSizeFull();

        Checkbox check = new Checkbox();
        Grid<Record> chronologicalGrid = initChronologicalGrid();
        Grid<RecordSpan> byTaskGrid = initByTaskGrid();
        TreeGrid<RecordSpanEntry> byTreeGrid = initByTreeGrid();

        tabs.add(chronological, chronologicalGrid);
        tabs.add(byTask, byTaskGrid);
        tabs.add(byTree, byTreeGrid);

        HorizontalLayout inputLayout = new HorizontalLayout(startDatePicker, endDatePicker, getAllButton);
        inputLayout.setWidthFull();

        this.add(inputLayout, tabs);
        this.setSizeFull();

        getAllButton.addClickListener(e -> {
            if (tabs.getSelectedTab().equals(byTask)) {
                Collection<RecordSpan> recordSpans = services.record()
                        .fetchRecordsDuringTimespanByRecord(startDatePicker.getValue(), endDatePicker.getValue())
                        .values();
                UI.getCurrent().access(() -> byTaskGrid.setItems(recordSpans));
            } else if (tabs.getSelectedTab().equals(chronological)) {
                List<Record> records = services.record()
                        .fetchRecordsDuringTimespanByTime(startDatePicker.getValue(), endDatePicker.getValue())
                        .toList();
                UI.getCurrent().access(() -> chronologicalGrid.setItems(records));
            } else if (tabs.getSelectedTab().equals(byTree)) {
                List<TaskNode> filteredNodes = services.query().fetchAllNodes((TaskNodeTreeFilter) new TaskNodeTreeFilter()
                                .completed(false)
                                .recurring(true)
                                .ignoreScheduling(true))
                        .toList();

                Map<TaskID, RecordSpan> recordMap = services.record()
                        .fetchRecordsDuringTimespanByRecordFromTasks(startDatePicker.getValue(), endDatePicker.getValue(),
                                filteredNodes.stream()
                                        .map(node -> node.task().id())
                                        .toList());

                log.debug("Record map size: " + recordMap.size());
                List<LinkID> filteredNodeIds = filteredNodes.stream()
                        .map(TaskNode::id)
                        .toList();

                Function<TaskID, List<RecordSpanEntry>> getChildren = parentId -> taskNetworkGraph.getChildren(
                        parentId, filteredNodeIds, null, null)
                        .map(node -> new RecordSpanEntry(node.id(),
                                recordMap.getOrDefault(node.task().id(),
                                        new RecordSpan(
                                                node.task(),
                                                new HashMap<>(),
                                                0,
                                                Duration.ZERO,
                                                Duration.ZERO))))
                        .toList();
                UI.getCurrent().access(() -> byTreeGrid.setItems(
                        getChildren.apply(null),
                        key -> getChildren.apply(key.task().id())));
            }
        });
    }

    private String percentColumnText(int count, int totalCount) {
        return count != 0
                ? count + " (" + Math.round(((float) count / totalCount)*100) + "%)"
                : "-";
    }

    private String formattedTime(LocalDateTime time) {
        return time != null
                ? time.format(DateTimeFormatter.ofPattern("MM/dd HH:mm a"))
                : "";
    }
    private Grid<Record> initChronologicalGrid() {
        Grid<Record> chronologicalGrid = new Grid<>(Record.class, false);
        chronologicalGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        chronologicalGrid.addColumn(Record::name)
                .setHeader("Task")
                .setResizable(true)
                .setFrozen(true)
                .setFlexGrow(1)
                .setSortable(true);

        chronologicalGrid.addColumn(record -> formattedTime(record.startTime()))
                .setHeader("Start")
                .setAutoWidth(true)
                .setSortable(true);

        chronologicalGrid.addColumn(record -> formattedTime(record.endTime()))
                .setHeader("End")
                .setAutoWidth(true)
                .setSortable(true);

        chronologicalGrid.addColumn(record -> DurationConverter.toPresentation(
                taskNetworkGraph.taskMap().get(record.taskId()).duration()))
                .setHeader("Est. Duration")
                .setAutoWidth(true);

        chronologicalGrid.addColumn(record -> DurationConverter.toPresentation(record.elapsedTime()))
                .setHeader("Elapsed")
                .setAutoWidth(true);

        chronologicalGrid.addColumn(record -> DurationConverter.toPresentation(
                taskNetworkGraph.taskMap().get(record.taskId()).duration()
                        .minus(record.elapsedTime())))
                .setHeader("Difference")
                .setAutoWidth(true);

        chronologicalGrid.addColumn(record -> DurationConverter.toPresentation(record.inactiveTime()))
                .setHeader("Inactive")
                .setAutoWidth(true);

        chronologicalGrid.setSizeFull();
        return chronologicalGrid;
    }

    private Grid<RecordSpan> initByTaskGrid() {
        Grid<RecordSpan> byTaskGrid = new Grid<>(RecordSpan.class, false);
        byTaskGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        byTaskGrid.addColumn(recordSpan -> recordSpan.task().name())
                .setHeader("Task Name")
                .setResizable(true)
                .setFrozen(true)
                .setFlexGrow(1)
                .setSortable(true);

        addRecordSpanColumns(byTaskGrid);

        byTaskGrid.setSizeFull();
        return byTaskGrid;

//        Editor<RecordSpan> editor = byTaskGrid.getEditor();
//
//        byTaskGrid.addColumn(LitRenderer.<RecordSpan>of(
//                                GridUtil.inlineVaadinIconLitExpression("edit",
//                                        "active"))
//                        .withFunction("onClick", t -> {
//                            if (editor.isOpen()) {
//                                editor.cancel();
//                            }
//                            editor.editItem(t);
//                        }))
//                .setKey(ColumnKey.EDIT.toString())
//                .setFrozen(true);
//
//        editor.addSaveListener(recordSpan -> {
//            services.record().persist(recordSpan.getItem());
//            byTaskGrid.setItems(services.record().fetchRecordsDuringTimespan(startDatePicker.getValue(), endDatePicker.getValue()).values());
//        });
    }

    private TreeGrid<RecordSpanEntry> initByTreeGrid() {
        TreeGrid<RecordSpanEntry> byTreeGrid = new TreeGrid<>(RecordSpanEntry.class);
        byTreeGrid.addThemeVariants(GridVariant.LUMO_COMPACT);

        byTreeGrid.addHierarchyColumn(recordSpan -> recordSpan.task().name())
                .setHeader("Task Name")
                .setResizable(true)
                .setFrozen(true)
                .setFlexGrow(1)
                .setSortable(true);

        addRecordSpanColumns(byTreeGrid);

        byTreeGrid.setSizeFull();
        return byTreeGrid;
    }

    private <T extends RecordSpan> void  addRecordSpanColumns(Grid<T> grid) {
        grid.addColumn(RecordSpan::totalCount)
                .setHeader("Total Count")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(recordSpan -> DurationConverter.toPresentation(recordSpan.averageDuration()))
                .setHeader("Average")
                .setAutoWidth(true)
                .setComparator(Comparator.comparing(recordSpan -> recordSpan.averageDuration() != null
                        ? recordSpan.averageDuration()
                        : Duration.ZERO));

        grid.addColumn(recordSpan ->  DurationConverter.toPresentation(
                        recordSpan.netDuration()))
                .setHeader("Duration")
                .setAutoWidth(true)
                .setComparator(Comparator.comparing(RecordSpan::netDuration));

        grid.addColumn(recordSpan -> (recordSpan.averageDuration() != null)
                        ? DurationConverter.toPresentation(
                        recordSpan.netDuration().minus(recordSpan.averageDuration()))
                        : K.DURATION_PLACEHOLDER)
                .setHeader("Difference")
                .setAutoWidth(true)
                .setComparator(Comparator.comparing(recordSpan -> {
                    if (recordSpan.averageDuration() != null) {
                        return recordSpan.netDuration().minus(recordSpan.averageDuration());
                    } else {
                        return Duration.ZERO;
                    }
                }));

        Set<TimeableStatus> timeableStatuses = Set.of(
                TimeableStatus.COMPLETED,
                TimeableStatus.POSTPONED,
                TimeableStatus.EXCLUDED,
                TimeableStatus.SKIPPED);

        for (TimeableStatus status : timeableStatuses) {
            grid.addColumn(recordSpan -> percentColumnText(recordSpan.resultMap().containsKey(status)
                                    ? recordSpan.resultMap().get(status).size()
                                    : 0,
                            recordSpan.totalCount()))
                    .setHeader(status.toString())
                    .setAutoWidth(true)
                    .setSortable(true);
        }
    }

}
