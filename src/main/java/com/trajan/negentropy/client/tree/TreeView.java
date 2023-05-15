package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.client.tree.components.FilterForm;
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
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Negentropy - Task Tree")
@UIScope
@Route(value = "", layout = MainLayout.class)
@RouteAlias("tree")
@Uses(Icon.class)
@Accessors(fluent = true)
@Getter
public class TreeView extends Div {
    private static final Logger logger = LoggerFactory.getLogger(TreeView.class);
    @Autowired private final TreeViewPresenter presenter;
    @Autowired private final SessionSettings settings;

    private final QuickCreateField quickAddField;
    private final FilterForm filterForm;
    private final TaskTreeGrid taskTreeGrid;
    private final TaskFormLayout createTaskForm;
    private final HorizontalLayout options;
    private final TabSheet tabSheet;

    private final int BREAKPOINT_PX = 600;

    public TreeView(TreeViewPresenter presenter, SessionSettings settings) {
        this.presenter = presenter;
        this.settings = settings;

        this.taskTreeGrid = new TaskTreeGrid(presenter, settings);

        this.addClassName("tree-view");
        this.setSizeFull();

        this.tabSheet = new TabSheet();

        this.quickAddField = new QuickCreateField(presenter);
        quickAddField.setWidthFull();

        this.filterForm = new FilterForm(presenter);
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        this.createTaskForm = new TaskFormLayout(presenter, new Task(null));
        createTaskForm.binder().setBean(new Task(null));

        createTaskForm.onClear(() -> {
            createTaskForm.binder().setBean(new Task(null));
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
                recalculateTimeEstimates);
        options.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver ->
                initTabSheet(receiver.getScreenWidth()));

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
        Tab closeTab;
        Tab quickCreateTab;
        Tab searchAndFilterTab;
        Tab createNewTaskTab;
        Tab optionsTab;

        if (browserWidth > BREAKPOINT_PX) {
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
        tabSheet.add(quickCreateTab, this.quickAddField);
        tabSheet.add(searchAndFilterTab, this.filterForm);
        tabSheet.add(createNewTaskTab, this.createTaskForm);
        tabSheet.add(optionsTab, this.options);

        tabSheet.addSelectedChangeListener(event -> {
            Tab openedTab = event.getSelectedTab();

            if (openedTab.equals(quickCreateTab)) {
                presenter.activeTaskProvider(quickAddField);
            } else if (openedTab.equals(createNewTaskTab)) {
                presenter.activeTaskProvider(createTaskForm);
            } else {
                presenter.activeTaskProvider(null);
            }
        });
    }
}