package com.trajan.negentropy.client.routine;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.RoutineDataProvider;
import com.trajan.negentropy.client.routine.components.RoutineCard;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.util.DoubleClickListenerUtil;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.model.Routine;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.grid.Grid;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

@PageTitle("Negentropy - Routine")
@UIScope
@Route(value = "routine", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@Accessors(fluent = true)
@Getter
public class RoutineView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(RoutineView.class);

    @Autowired private ClientDataController controller;
    @Autowired private SessionSettings settings;
    @Autowired private RoutineDataProvider routineDataProvider;
    @Autowired private RoutineService routineService;

    private VerticalLayout routineDiv;

    Grid<Routine> routineSelectionGrid = new Grid<>(Routine.class, false);

    @PostConstruct
    public void init() {
        this.addClassName("routine-view");

        routineDiv = new VerticalLayout();

        routineDataProvider.fetch(new Query<>(
                Set.of(RoutineStatus.ACTIVE)))
                .forEachOrdered(routine ->
                        routineDiv.add(new RoutineCard(routine, controller)));

        routineSelectionGrid.setItems(routineDataProvider.fetch(new Query<>(
                Set.of(RoutineStatus.NOT_STARTED))).toList());

        initRoutineSelectionGrid();

        this.add(routineDiv, routineSelectionGrid);
    }

    private void initRoutineSelectionGrid() {
        routineSelectionGrid.addColumn(r -> r.steps().get(0).task().name());

        DoubleClickListenerUtil.add(routineSelectionGrid, routine -> {
            RoutineResponse response = controller.startRoutineStep(routine.currentStep().id());
            routineDiv.add(new RoutineCard(response.routine(), controller));
        });
    }

}