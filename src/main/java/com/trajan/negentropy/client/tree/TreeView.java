package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.components.ToolbarTabSheet;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Negentropy - Task Tree")
@UIScope
@Route(value = "tree", layout = MainLayout.class)
@Uses(Icon.class)
@Slf4j
@Accessors(fluent = true)
@Getter
public class TreeView extends Div {
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;

    @Autowired private ToolbarTabSheet toolbarTabSheet;

    @Autowired private TaskEntryTreeGrid taskTreeGrid;

    @PostConstruct
    public void init() {
        this.addClassName("tree-view");
        this.setSizeFull();

        toolbarTabSheet.initWithAllTabs();

        // TODO: Not yet implemented, option to move or add on drag
        Select<String> dragSettings = new Select<>();
        dragSettings.add("Move on drag");
        dragSettings.add("Add on drag");

        taskTreeGrid.init(settings.treeViewColumnVisibility());
        taskTreeGrid.treeGrid().setDataProvider(controller.dataProvider());
        taskTreeGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(
                toolbarTabSheet,
                taskTreeGrid);

        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
    }
}