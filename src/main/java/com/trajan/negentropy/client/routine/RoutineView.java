package com.trajan.negentropy.client.routine;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.ToolbarTabSheet;
import com.trajan.negentropy.client.components.grid.RoutineStepTreeGrid;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.RoutineDataProvider;
import com.trajan.negentropy.client.routine.components.RoutineCard;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.model.Routine;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@PageTitle("Negentropy - Routine")
@Slf4j
@UIScope
@Route(value = "routine", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@Accessors(fluent = true)
@Getter
public class RoutineView extends VerticalLayout {
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;
    @Autowired private RoutineDataProvider routineDataProvider;
    @Autowired private RoutineService routineService;

    @Autowired private RoutineStepTreeGrid routineStepTreeGrid;
    @Autowired private ToolbarTabSheet toolbarTabSheet;
    private Grid<Routine> activeRoutineGrid = new Grid<>();

    @PostConstruct
    public void init() {
        this.addClassName("routine-view");
        this.setSizeFull();
        this.setJustifyContentMode(JustifyContentMode.START);

        Set<TimeableStatus> visibleRoutineStatuses = Set.of(
                TimeableStatus.NOT_STARTED,
                TimeableStatus.ACTIVE);

        toolbarTabSheet.init(() -> routineStepTreeGrid.clearRoutine(),
                ToolbarTabSheet.TabType.CLOSE_TAB,
                ToolbarTabSheet.TabType.SEARCH_AND_FILTER_TAB,
                ToolbarTabSheet.TabType.OPTIONS_TAB);

        routineStepTreeGrid.init(settings.routineViewColumnVisibility());
        routineStepTreeGrid.setSizeFull();

        activeRoutineGrid.setItems(routineDataProvider.fetch(new Query<>(
                visibleRoutineStatuses)).toList());

        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        activeRoutineGrid.addComponentColumn(routine -> new RoutineCard(routine, controller));
        activeRoutineGrid.setAllRowsVisible(true); // TODO: Verify this works
        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);

        activeRoutineGrid.addSelectionListener(event ->
                event.getFirstSelectedItem().ifPresent(routine -> {
                    routineStepTreeGrid.setRoutine(routine);
                })
        );
        activeRoutineGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        this.add(activeRoutineGrid, toolbarTabSheet, routineStepTreeGrid);
    }

}