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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import java.time.LocalDateTime;
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

        initRoutineSelectionGrid();

        this.add(activeRoutineGrid, routineSelectionGrid);
    }

    private HorizontalLayout createCard(Routine routine) {
        logger.debug("Creating card for routine: " + routine);
        HorizontalLayout card = new HorizontalLayout();
        card.addClassName("card");
        card.setSpacing(false);
        card.getThemeList().add("spacing-s");

        VerticalLayout middle = new VerticalLayout();
        middle.addClassName("middle");
        middle.setSpacing(false);
        middle.setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setSpacing(false);
        header.getThemeList().add("spacing-s");
        header.setWidthFull();


        Label hey = new Label("HEY!");
//        hey.addClassName("name");

        Span name = new Span(routine.currentStep().task().name());
        name.addClassName("name");
        Span date = new Span(String.valueOf(LocalDateTime.now()));
        date.addClassName("date");
        header.add(hey, name, date);

        Span post = new Span(routine.currentStep().task().description());
        post.addClassName("post");

        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("actions");
        actions.setSpacing(false);
        actions.getThemeList().add("spacing-s");

        Icon likeIcon = VaadinIcon.HEART.create();
        likeIcon.addClassName("icon");
//        Span likes = new Span(person.getLikes());
//        likes.addClassName("likes");
        Icon commentIcon = VaadinIcon.COMMENT.create();
        commentIcon.addClassName("icon");
//        Span comments = new Span(person.getComments());
//        comments.addClassName("comments");
        Icon shareIcon = VaadinIcon.CONNECT.create();
        shareIcon.addClassName("icon");
//        Span shares = new Span(person.getShares());
//        shares.addClassName("shares");

        actions.add(likeIcon, commentIcon, shareIcon);

        middle.add(header, post, actions);
        card.add(middle);
        return card;
    }

    private void initRoutineSelectionGrid() {
        routineSelectionGrid.addColumn(r -> r.steps().get(0).task().name());

        DoubleClickListenerUtil.add(routineSelectionGrid, routine -> {
            RoutineResponse response = controller.startRoutineStep(routine.currentStep().id());
            routineDiv.add(new RoutineCard(response.routine(), controller));
        });
    }

}