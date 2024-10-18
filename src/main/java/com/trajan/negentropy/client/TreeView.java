package com.trajan.negentropy.client;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet.TabType;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.TaskNetworkGraph;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.BannerProvider;
import com.trajan.negentropy.model.id.LinkID;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.*;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Task Tree")
@Route(value = "tree", layout = MainLayout.class)
@Uses(Icon.class)
@Getter
@Benchmark(millisFloor = 10)
public class TreeView extends Div implements HasUrlParameter<String> {
    private final UILogger log = new UILogger();

    @Autowired private BannerProvider bannerProvider;
    @Autowired private UIController controller;
    @Autowired private UserSettings settings;
    @Autowired private TaskNetworkGraph networkGraph;

    @Autowired private ToolbarTabSheet toolbarTabSheet;

    private FlexLayout gridLayout = new FlexLayout();

    @Autowired private TaskEntryTreeGrid taskTreeGrid;

    @PostConstruct
    public void init() {
        log.info("Initializing TreeView");
        this.addClassName("tree-view");

        controller.activeTaskNodeDisplay(taskTreeGrid);

        toolbarTabSheet.init(this,
                TabType.CLOSE_TAB,
                TabType.CREATE_NEW_TASK_TAB_FULL,
                TabType.INSERT_TASK_TAB,
                TabType.SEARCH_AND_FILTER_TAB,
                TabType.OPTIONS_TAB,
                TabType.QUICK_CREATE_TAB);

        // TODO: Not yet implemented, option to move or add on drag
        Select<String> dragSettings = new Select<>();
        dragSettings.add("Move on drag");
        dragSettings.add("Add on drag");

        if (taskTreeGrid.grid() == null) {
            taskTreeGrid.init(settings.treeViewColumnVisibilities().get(0),
                    settings.gridSelectionMode());
        }
        taskTreeGrid.setWidthFull();
//            taskTreeGrid.setHeight("86%");

//        setGridTiling(settings.gridTiling());
        gridLayout.add(taskTreeGrid);

        VerticalLayout layout = new VerticalLayout(
                toolbarTabSheet,
                gridLayout);

        gridLayout.setSizeFull();
        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
        this.setSizeFull();
    }

//    public void setGridTiling(GridTiling gridTiling) {
//        gridLayout.removeAll();
//        switch (gridTiling) {
//            case NONE -> {
//                gridLayout.add(taskTreeGrid);
//            }
//            case VERTICAL -> {
//                gridLayout.add(taskTreeGrid, secondTaskTreeGrid);
//                gridLayout.setFlexDirection(FlexLayout.FlexDirection.ROW);
//            }
//            case HORIZONTAL -> {
//                gridLayout.add(taskTreeGrid, secondTaskTreeGrid);
//                gridLayout.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
//            }
//        }
//    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter != null) {
            LinkID linkId = new LinkID(Long.parseLong(parameter));
            if (!taskTreeGrid.nestedTabs().selectNewRootNode(linkId)) {
                // TODO: This loops infinitely
                log.warn("Task node with ID " + parameter + " was not found among task entries.");
//                log.debug("Task node with ID " + parameter + " was not found; trying the long way.");
//                TaskNode result = networkGraph.nodeMap().get(linkId);
//                if (result != null) {
//                    TaskEntryDataProvider dataProvider = taskTreeGrid.taskEntryDataProvider();
//                    List<TaskNode> ancestors = SpringContext.getBean(QueryService.class).fetchAncestorNodes(
//                                    result.task().id(), settings.filter())
//                            .toList();
//
//                    Supplier<List<TaskEntry>> getMatchingEntries = () -> ancestors.stream()
//                            .map(TaskNode::id)
//                            .filter(id -> dataProvider.linkTaskEntriesMap().containsKey(id))
//                            .map(dataProvider.linkTaskEntriesMap()::get)
//                            .flatMap(List::stream)
//                            .toList();
//
//                    List<TaskEntry> matchingEntries = getMatchingEntries.get();
//                    while (!matchingEntries.isEmpty()) {
//                        taskTreeGrid.grid().expand(matchingEntries);
//                        matchingEntries = getMatchingEntries.get();
//                        matchingEntries = matchingEntries.stream()
//                                .filter(entry -> taskTreeGrid.grid().isExpanded(entry))
//                                .toList();
//                    }
//                    if (!taskTreeGrid.nestedTabs().selectNewRootNode(linkId)) {
//                        UI.getCurrent().access(() -> {
//                            NotificationMessage.error("Task node with ID " + parameter + " was not found among task entries.");
//                        });
//                    }
//                } else {
//                    UI.getCurrent().access(() -> {
//                        NotificationMessage.error("Task node with ID " + parameter + " was not found as a task node.");
//                    });
//                }
            }
        }
    }
}