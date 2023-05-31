package com.trajan.negentropy.client.routine;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.RoutineDataProvider;
import com.trajan.negentropy.client.routine.components.RoutineCard;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.server.backend.entity.TimeableStatus;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
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
    @Autowired private SessionSettings settings;
    @Autowired private RoutineDataProvider routineDataProvider;
    @Autowired private RoutineService routineService;

    private VerticalLayout routineDiv;

    private Grid<Routine> routineSelectionGrid = new Grid<>(Routine.class, false);
    private Grid<Routine> activeRoutineGrid = new Grid<>();

    @PostConstruct
    public void init() {
        this.addClassName("routine-view");
        this.setSizeFull();

        routineSelectionGrid.setItems(routineDataProvider.fetch(new Query<>(
                Set.of(TimeableStatus.NOT_STARTED))).toList());

        activeRoutineGrid.setItems(routineDataProvider.fetch(new Query<>(
                Set.of(TimeableStatus.ACTIVE))).toList());

//        activeRoutineGrid.setHeight("100%");
        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        activeRoutineGrid.addComponentColumn(routine -> new RoutineCard(routine, controller));
        activeRoutineGrid.setAllRowsVisible(true);
        activeRoutineGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);

        initRoutineSelectionGrid();

        // TODO: Verify this works
        activeRoutineGrid.setAllRowsVisible(true);
        routineSelectionGrid.setAllRowsVisible(true);

        this.add(activeRoutineGrid, routineSelectionGrid);
    }

    private void initRoutineSelectionGrid() {
        routineSelectionGrid.addColumn(r -> r.steps().get(0).task().name());

        DoubleClickListenerUtil.add(routineSelectionGrid, routine -> {
            RoutineResponse response = controller.startRoutineStep(routine.currentStep().id());
            routineDiv.add(new RoutineCard(response.routine(), controller));
        });
    }

}