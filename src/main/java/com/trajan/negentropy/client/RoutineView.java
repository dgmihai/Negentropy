package com.trajan.negentropy.client;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.components.routine.RoutineCard;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet.TabType;
import com.trajan.negentropy.client.components.wellness.MoodInput;
import com.trajan.negentropy.client.components.wellness.StressorInput;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.RoutineDataProvider;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PageTitle("Routines")
@Route(value = "routine", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@UIScope
@Getter
@Benchmark(millisFloor = 10)
public class RoutineView extends Div {
    private final UILogger log = new UILogger();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final VerticalLayout content = new VerticalLayout();

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
        log.info("Initializing RoutineView");
        this.setSizeFull();
        this.add(content);
        content.addClassName("routine-view");
        content.setSizeFull();
        content.setJustifyContentMode(JustifyContentMode.START);

        toolbarTabSheet.init(this, () -> routineStepTreeGrid.clearRoutine(),
                TabType.HIDE_ROUTINE_STEPS_TAB,
                TabType.SHOW_ROUTINE_STEPS_TAB,
//                TabType.CREATE_NEW_TASK_TAB_FULL,
//                TabType.INSERT_TASK_TAB,
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

        this.refreshRoutines();

        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        activeRoutineGrid.addComponentColumn(routine -> new RoutineCard(routine, controller, routineStepTreeGrid));
        activeRoutineGrid.setAllRowsVisible(true); // TODO: Verify this works
        activeRoutineGrid.setClassName("active-routine-grid");
        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);

        activeRoutineGrid.addSelectionListener(event ->
                event.getFirstSelectedItem().ifPresent(routine -> {
                    CompletableFuture.runAsync(() -> {
                        routineStepTreeGrid.setRoutine(routine.id());
                        activeRoutineGrid.select(routine);
                    });
                })
        );
        activeRoutineGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        FormLayout mentalStateLayout = new FormLayout();
        mentalStateLayout.add(
                SpringContext.getBean(StressorInput.class),
                SpringContext.getBean(MoodInput.class));
        mentalStateLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(K.SHORT_SCREEN_WIDTH, 2));
        mentalStateLayout.setWidthFull();

        content.add(mentalStateLayout, activeRoutineGrid, toolbarTabSheet, routineStepTreeGrid);
    }

    public Routine getActiveRoutine() {
        Set<Routine> selectedRoutines = activeRoutineGrid.getSelectedItems();
        return !selectedRoutines.isEmpty() ? activeRoutineGrid.getSelectedItems().iterator().next() : null;
    }

    public void refreshRoutines() {
        executor.execute(() -> {
            List<Routine> routines = routineDataProvider.fetch(new Query<>(
                    visibleRoutineStatuses)).toList();
            try {
                controller.accessUI(() -> activeRoutineGrid.setItems(routines));
            } catch (Exception e) {
                log.error("Error refreshing routines", e);
                NotificationMessage.error("Error refreshing routines");
            }
        });
    }
}