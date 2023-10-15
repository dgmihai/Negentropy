package com.trajan.negentropy.client;

import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.sessionlogger.SessionLogged;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.RecordSpan;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;


@PageTitle("Records")
@UIScope
@Route(value = "records", layout = MainLayout.class)
@Uses(Icon.class)
public class RecordView extends VerticalLayout implements SessionLogged {
    @Autowired
    @Getter private SessionLoggerFactory loggerFactory;
    @Getter protected SessionLogger log;

    @Autowired private SessionServices services;

    @PostConstruct
    public void init() {
        log = getLogger(this.getClass());

        this.addClassName("record-view");
        this.setSizeFull();

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPlaceholder("From");
        startDatePicker.setClearButtonVisible(true);
        startDatePicker.setValue(LocalDate.now().minusDays(7));

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPlaceholder("To");
        endDatePicker.setClearButtonVisible(true);
        endDatePicker.setValue(LocalDate.now());

        Button getAllButton = new Button(VaadinIcon.CHECK.create());

        ProgressBar progressBar = new ProgressBar();

        Grid<RecordSpan> grid = new Grid<>(RecordSpan.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        grid.addColumn(recordSpan -> recordSpan.task().name())
                .setHeader("Task Name")
                .setResizable(true)
                .setFrozen(true)
                .setFlexGrow(1)
                .setSortable(true);

//        Editor<RecordSpan> editor = grid.getEditor();
//
//        grid.addColumn(LitRenderer.<RecordSpan>of(
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
//            grid.setItems(services.record().fetchRecordsDuringTimespan(startDatePicker.getValue(), endDatePicker.getValue()).values());
//        });

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

        getAllButton.addClickListener(buttonClickEvent -> {
            services.record().fetchRecordsDuringTimespan(startDatePicker.getValue(), endDatePicker.getValue());
        });

        HorizontalLayout topLayout = new HorizontalLayout(startDatePicker, endDatePicker, getAllButton);
        topLayout.setWidthFull();
        topLayout.setAlignItems(Alignment.BASELINE);

        this.add(topLayout, progressBar, grid);
        grid.setWidthFull();

        getAllButton.addClickListener(e -> {
            progressBar.setIndeterminate(true);
            Collection<RecordSpan> recordSpans = services.record()
                    .fetchRecordsDuringTimespan(startDatePicker.getValue(), endDatePicker.getValue())
                    .values();
            UI.getCurrent().access(() -> {
                grid.setItems(recordSpans);
                progressBar.setIndeterminate(false);
            });
        });
    }

    private String percentColumnText(int count, int totalCount) {
        return count != 0
                ? count + " (" + Math.round(((float) count / totalCount)*100) + "%)"
                : "-";
    }

}
