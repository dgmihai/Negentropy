package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet.TabType;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.TaskNetworkGraph;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.session.enums.GridTiling;
import com.trajan.negentropy.client.util.BannerProvider;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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

import java.util.List;

@PageTitle("Negentropy - Task Tree")
@UIScope
@Route(value = "tree", layout = MainLayout.class)
@Uses(Icon.class)
@Slf4j
@Accessors(fluent = true)
@Getter
public class TreeView extends Div {
    @Autowired private BannerProvider bannerProvider;
    @Autowired private ClientDataController controller;
    @Autowired private UserSettings settings;
    @Autowired private TaskNetworkGraph networkGraph;

    @Autowired private ToolbarTabSheet toolbarTabSheet;

    private FlexLayout gridLayout = new FlexLayout();

    @Autowired private TaskEntryTreeGrid firstTaskTreeGrid;
    private TaskEntryTreeGrid secondTaskTreeGrid = null;

    @PostConstruct
    public void init() {
        controller.sync();
        this.addClassName("tree-view");

//        List<TaskEntryTreeGrid> grids = List.of(firstTaskTreeGrid, secondTaskTreeGrid);
        List<TaskEntryTreeGrid> grids = List.of(firstTaskTreeGrid);
        controller.activeTaskNodeView(firstTaskTreeGrid);

        toolbarTabSheet.init(this,
                TabType.CLOSE_TAB,
                TabType.CREATE_NEW_TASK_TAB,
                TabType.INSERT_TASK_TAB,
                TabType.SEARCH_AND_FILTER_TAB,
                TabType.OPTIONS_TAB,
                TabType.QUICK_CREATE_TAB);

        // TODO: Not yet implemented, option to move or add on drag
        Select<String> dragSettings = new Select<>();
        dragSettings.add("Move on drag");
        dragSettings.add("Add on drag");

        for (int i=0; i<grids.size(); i++) {
            TaskEntryTreeGrid grid = grids.get(i);
            grid.init(settings.treeViewColumnVisibilities().get(i),
                    settings.gridSelectionMode());
            grid.setWidthFull();
            grid.setHeight("86%");
        }

        setGridTiling(settings.gridTiling());

        VerticalLayout layout = new VerticalLayout(
                toolbarTabSheet,
                gridLayout);

        gridLayout.setSizeFull();
        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
        this.setSizeFull();
    }

    public void setGridTiling(GridTiling gridTiling) {
        gridLayout.removeAll();
        switch (gridTiling) {
            case NONE -> gridLayout.add(firstTaskTreeGrid);
            case VERTICAL -> {
                gridLayout.add(firstTaskTreeGrid, secondTaskTreeGrid);
                gridLayout.setFlexDirection(FlexLayout.FlexDirection.ROW);
            }
            case HORIZONTAL -> {
                gridLayout.add(firstTaskTreeGrid, secondTaskTreeGrid);
                gridLayout.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
            }
        }
    }
}