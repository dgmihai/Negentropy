package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.quickadd.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.tree.components.FilterLayout;
import com.trajan.negentropy.server.facade.model.Task;
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
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PageTitle("Negentropy - Task Tree")
@Route(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@RouteAlias("tree")
@Accessors(fluent = true)
@Getter
public class TreeView extends Div {
    private static final Logger logger = LoggerFactory.getLogger(TreeView.class);
    private final TreeViewPresenter presenter;

    private final QuickCreateField quickAddField;
    private final FilterLayout filterDiv;
    private final TaskTreeGrid taskTreeGrid;
    private final TaskFormLayout createTaskForm;
    private final HorizontalLayout options;
    private TabSheet tabSheet;

    private final int BREAKPOINT_PX = 600;

    public TreeView(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.taskTreeGrid = new TaskTreeGrid(presenter);
        this.presenter.initTreeView(this);

        this.addClassName("tree-view");
        this.setSizeFull();

        this.tabSheet = new TabSheet();

        this.quickAddField = new QuickCreateField();
        quickAddField.onAction(task -> presenter.addTaskFromProvider(quickAddField));
        quickAddField.setWidthFull();

        this.filterDiv = new FilterLayout(presenter);

        this.createTaskForm = new TaskFormLayout(presenter, new Task(null));
        createTaskForm.binder().setBean(new Task(null));

        createTaskForm.onClear(() -> {
            createTaskForm.binder().setBean(new Task(null));
            tabSheet.setSelectedIndex(0);
        });
        createTaskForm.onSave(() -> {
            presenter.addTaskFromProvider(createTaskForm);
            createTaskForm.binder().setBean(new Task(null));
        });
        createTaskForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        Button recalculateTimeEstimates = new Button("Recalculate Time Estimates");
        recalculateTimeEstimates.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recalculateTimeEstimates.addClickListener(e -> presenter.recalculateTimeEstimates());

        // TODO: Not yet implemented, option to move or add on drag
        Select<String> dragSettings = new Select<>();
        dragSettings.add("Move on drag");
        dragSettings.add("Add on drag");

        this.options = new HorizontalLayout(
                recalculateTimeEstimates,
                taskTreeGrid.visibilityMenu());
        options.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver -> {
            initTabSheet(receiver.getScreenWidth());
        });

        tabSheet.setWidthFull();

        tabSheet.addThemeVariants(TabSheetVariant.LUMO_TABS_CENTERED);
        taskTreeGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(
                tabSheet,
                taskTreeGrid);

        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
    }

    private void initTabSheet(int browserWidth) {
        Tab close;
        Tab quickAdd;
        Tab searchAndFilter;
        Tab createNewTask ;
        Tab options;

        if (browserWidth > BREAKPOINT_PX) {
            close = new Tab(VaadinIcon.CLOSE_SMALL.create());
            quickAdd = new Tab("Quick Add");
            searchAndFilter = new Tab("Search & Filter");
            createNewTask = new Tab("Create New Task");
            options = new Tab("Options");
        } else {
            close = new Tab(VaadinIcon.CLOSE.create());
            quickAdd = new Tab(VaadinIcon.BOLT.create());
            searchAndFilter = new Tab(VaadinIcon.SEARCH.create());
            createNewTask = new Tab(VaadinIcon.FILE_ADD.create());
            options = new Tab(VaadinIcon.COG.create());
        }

        tabSheet.add(close, new Div());
        tabSheet.add(quickAdd, quickAddField);
        tabSheet.add(searchAndFilter, filterDiv());
        tabSheet.add(createNewTask, createTaskForm);
        tabSheet.add(options, options);

        tabSheet.addSelectedChangeListener(event -> {
            Tab openedTab = event.getSelectedTab();

            if (openedTab.equals(quickAdd)) {
                presenter.activeTaskProvider(quickAddField);
            } else if (openedTab.equals(createNewTask)) {
                presenter.activeTaskProvider(createTaskForm);
            } else {
                presenter.activeTaskProvider(null);
            }
        });
    }
}