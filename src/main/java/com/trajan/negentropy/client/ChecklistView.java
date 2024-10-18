package com.trajan.negentropy.client;

import com.trajan.negentropy.client.components.grid.RoutineStepGridUtil;
import com.trajan.negentropy.client.components.routine.ChecklistGrid;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.RoutineDataProvider;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.id.RoutineID;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Checklist")
@Route(value = "checklist", layout = MainLayout.class)
@Uses(Icon.class)
@UIScope
public class ChecklistView extends Div {
    @Autowired private SessionServices services;
    private final UILogger log = new UILogger();

    private final VerticalLayout content = new VerticalLayout();

    @Autowired private UIController controller;
    @Autowired
    private RoutineDataProvider routineDataProvider;

    @Autowired private ChecklistGrid checklistGrid;

    @PostConstruct
    public void init() {
        log.info("Initializing ChecklistView");
        this.setSizeFull();
        content.addClassName("checklist-view");
        content.setSizeFull();
        content.setJustifyContentMode(JustifyContentMode.START);

        checklistGrid.init();
        checklistGrid.addClassName("checklist-grid");
        checklistGrid.setSizeFull();

        RoutineID activeRoutineId = controller.services().routine().activeRoutineId();
        if (activeRoutineId != null) {
            Routine activeRoutine = routineDataProvider.fetch(activeRoutineId);
            checklistGrid.setRoutine(activeRoutine);
        }

        content.add(RoutineStepGridUtil.emotionalTrackerLayout(), checklistGrid);
        this.add(content);
    }
}
