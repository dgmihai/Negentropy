package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.components.tasktreegrid.TaskTreeGrid;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.tree.components.FilterForm;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.util.ExecTimer;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
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
    @Autowired private ExecTimer execTimer;

    @Autowired private ClientDataController controller;
    @Autowired private SessionSettings settings;

    private QuickCreateField quickCreateField;
    private FilterForm filterForm;
    @Autowired private TaskTreeGrid taskTreeGrid;
    private TaskFormLayout createTaskForm;
    private HorizontalLayout options;
    private TabSheet tabSheet;

    @PostConstruct
    public void init() {
        execTimer.mark("TreeView init");

        this.addClassName("tree-view");
        this.setSizeFull();

        execTimer.mark("Tab Sheet");
        this.tabSheet = new TabSheet();

        execTimer.mark("Quick Create");
        this.quickCreateField = new QuickCreateField(controller);
        quickCreateField.setWidthFull();

        execTimer.mark("Filter Form");
        this.filterForm = new FilterForm(controller);
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        execTimer.mark("Task Form");
        this.createTaskForm = new TaskFormLayout(controller, new Task(null));
        createTaskForm.binder().setBean(new Task(null));

        createTaskForm.onClear(() -> {
            createTaskForm.binder().setBean(new Task(null));
        });
        createTaskForm.onSave(() -> {
            controller.addTaskFromProvider(createTaskForm);
            createTaskForm.binder().setBean(new Task(null));
        });

        createTaskForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        execTimer.mark("Recalculate Time Estimates");
        Button recalculateTimeEstimates = new Button("Recalculate Time Estimates");
        recalculateTimeEstimates.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recalculateTimeEstimates.addClickListener(e -> controller.recalculateTimeEstimates());

        execTimer.mark("Drag Settings");
        // TODO: Not yet implemented, option to move or add on drag
        Select<String> dragSettings = new Select<>();
        dragSettings.add("Move on drag");
        dragSettings.add("Add on drag");

        execTimer.mark("Layout");
        this.options = new HorizontalLayout(
                recalculateTimeEstimates);
        options.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver ->
                initTabSheet(receiver.getScreenWidth()));

        tabSheet.setWidthFull();

        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_CENTERED);

        taskTreeGrid.setAllColumns();
        taskTreeGrid.treeGrid().setDataProvider(controller.dataProvider());
        taskTreeGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(
                tabSheet,
                taskTreeGrid);

        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);

        log.trace(execTimer.print());
    }

    private void initTabSheet(int browserWidth) {
        Tab closeTab;
        Tab quickCreateTab;
        Tab searchAndFilterTab;
        Tab createNewTaskTab;
        Tab optionsTab;

        if (browserWidth > K.BREAKPOINT_PX) {
            closeTab = new Tab(VaadinIcon.CLOSE_SMALL.create());
            quickCreateTab = new Tab(K.QUICK_CREATE);
            searchAndFilterTab = new Tab("Search & Filter");
            createNewTaskTab = new Tab("Create New Task");
            optionsTab = new Tab("Options");
        } else {
            closeTab = new Tab(VaadinIcon.CLOSE.create());
            quickCreateTab = new Tab(VaadinIcon.BOLT.create());
            searchAndFilterTab = new Tab(VaadinIcon.SEARCH.create());
            createNewTaskTab = new Tab(VaadinIcon.FILE_ADD.create());
            optionsTab = new Tab(VaadinIcon.COG.create());
        }

        tabSheet.add(closeTab, new Div());
        tabSheet.add(quickCreateTab, this.quickCreateField);
        tabSheet.add(searchAndFilterTab, this.filterForm);
        tabSheet.add(createNewTaskTab, this.createTaskForm);
        tabSheet.add(optionsTab, this.options);

        tabSheet.addSelectedChangeListener(event -> {
            Tab openedTab = event.getSelectedTab();

            if (openedTab.equals(quickCreateTab)) {
                controller.activeTaskProvider(quickCreateField);
            } else if (openedTab.equals(createNewTaskTab)) {
                controller.activeTaskProvider(createTaskForm);
            } else {
                controller.activeTaskProvider(null);
            }
        });
    }
}