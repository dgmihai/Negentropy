package com.trajan.negentropy.client.routine;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.components.routine.RoutineCard;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet.TabType;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.session.RoutineDataProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.sessionlogger.SessionLogger;
import com.trajan.negentropy.client.sessionlogger.SessionLoggerFactory;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.model.Mood;
import com.trajan.negentropy.model.entity.Emotion;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@PageTitle("Routine")
@UIScope
@Route(value = "routine", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@Accessors(fluent = true)
@Getter
public class RoutineView extends VerticalLayout {
    @Autowired private SessionLoggerFactory loggerFactory;
    private SessionLogger log;

    @Autowired private BannerProvider bannerProvider;
    @Autowired private UIController controller;
    @Autowired private UserSettings settings;
    @Autowired private RoutineDataProvider routineDataProvider;
    @Autowired private RoutineService routineService;

    @Autowired private RoutineStepTreeGrid routineStepTreeGrid;
    @Autowired private ToolbarTabSheet toolbarTabSheet;
    @Autowired private TimeableUtil timeableUtil;

    private final Grid<Routine> activeRoutineGrid = new Grid<>();

    private final Set<TimeableStatus> visibleRoutineStatuses = Set.of(
            TimeableStatus.NOT_STARTED,
            TimeableStatus.ACTIVE);

    @PostConstruct
    public void init() {
        log = loggerFactory.getLogger(this.getClass());

        this.addClassName("routine-view");
        this.setSizeFull();
        this.setJustifyContentMode(JustifyContentMode.START);

        toolbarTabSheet.init(this, () -> routineStepTreeGrid.clearRoutine(),
                TabType.HIDE_ROUTINE_STEPS_TAB,
                TabType.SHOW_ROUTINE_STEPS_TAB,
                TabType.CREATE_NEW_TASK_TAB_MINOR,
                TabType.INSERT_TASK_TAB,
                TabType.START_ROUTINE_TAB,
                TabType.OPTIONS_TAB);

        routineStepTreeGrid.setVisible(settings.routineStepsGridVisible());
        routineStepTreeGrid.init(settings.routineViewColumnVisibility(), SelectionMode.NONE);
        routineStepTreeGrid.addClassName("routine-step-tree-grid");
        routineStepTreeGrid.setSizeFull();

        toolbarTabSheet.addSelectedChangeListener(event -> {
            Tab tab = event.getSelectedTab();
            if (tab.equals(toolbarTabSheet.showRoutineStepsTab())) {
                settings.routineStepsGridVisible(true);
                routineStepTreeGrid.setVisible(true);
            } else if (tab.equals(toolbarTabSheet.hideRoutineStepsTab())) {
                settings.routineStepsGridVisible(false);
                routineStepTreeGrid.setVisible(false);
            }
        });

        refreshRoutines();

        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        activeRoutineGrid.addComponentColumn(routine -> new RoutineCard(routine, controller, routineStepTreeGrid));
        activeRoutineGrid.setAllRowsVisible(true); // TODO: Verify this works
        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);

        activeRoutineGrid.addSelectionListener(event ->
                event.getFirstSelectedItem().ifPresent(routine -> {
                    routineStepTreeGrid.setRoutine(routine.id());
                    activeRoutineGrid.select(routine);
                })
        );
        activeRoutineGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        this.add(new MoodInput(), activeRoutineGrid, toolbarTabSheet, routineStepTreeGrid);
    }

    public Routine getActiveRoutine() {
        Set<Routine> selectedRoutines = activeRoutineGrid.getSelectedItems();
        return !selectedRoutines.isEmpty() ? activeRoutineGrid.getSelectedItems().iterator().next() : null;
    }

    public void refreshRoutines() {
        List<Routine> routines = routineDataProvider.fetch(new Query<>(
                visibleRoutineStatuses)).toList();
        activeRoutineGrid.setItems(routines);
    }

    public class MoodInput extends HorizontalLayout {
        private final ComboBox<Emotion> emotionField = new ComboBox<>();

        public MoodInput() {
            this.addClassName("mood-input");
            this.setWidthFull();

            Mood lastMood = controller.services().mood().getLastMood();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

            emotionField.addClassName("emotion-field");
            emotionField.setLabel("What is your mood?");
            emotionField.setPlaceholder(lastMood.emotion() + " at " +  lastMood.timestamp().format(formatter));
            emotionField.setItemLabelGenerator(Emotion::toString);
            emotionField.setItems(Emotion.values());

            emotionField.addValueChangeListener(event -> {
                if (event.isFromClient()) {
                    controller.services().mood().persist(
                            new Mood(event.getValue()));
                    emotionField.setPlaceholder(event.getValue().toString());
                    bannerProvider.showRandomTenet();
                    emotionField.clear();
                }
            });

            this.add(emotionField);
        }
    }
}