package com.trajan.negentropy.client.components;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.SessionSettings;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.concurrent.atomic.AtomicBoolean;

public class ToolbarTabSheet extends TabSheet {
    private ClientDataController controller;
    private SessionSettings settings;

    private Tab closeTab;
    private Tab quickCreateTab;
    private Tab searchAndFilterTab;
    private Tab createNewTaskTab;
    private Tab optionsTab;

    private AtomicBoolean mobile = new AtomicBoolean();
    private TaskFormLayout createTaskForm;
    private QuickCreateField quickCreateField;
    private FilterForm filterForm;
    private HorizontalLayout options;

    public ToolbarTabSheet(ClientDataController controller, SessionSettings settings) {
        this.controller = controller;
        this.settings = settings;

        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver ->
                mobile.set(receiver.getScreenWidth() > K.BREAKPOINT_PX));

        initCloseTab();

        this.setWidthFull();
        this.addThemeVariants(TabSheetVariant.LUMO_TABS_CENTERED);
    }

    private ToolbarTabSheet initWidth(int browserWidth) {
        if (browserWidth > K.BREAKPOINT_PX) {
            createNewTaskTab = new Tab("Create New Task");
            closeTab = new Tab(VaadinIcon.CLOSE_SMALL.create());
            quickCreateTab = new Tab(K.QUICK_CREATE);
            searchAndFilterTab = new Tab("Search & Filter");
            optionsTab = new Tab("Options");
        } else {
            closeTab = new Tab(VaadinIcon.CLOSE.create());
            createNewTaskTab = new Tab(VaadinIcon.FILE_ADD.create());
            quickCreateTab = new Tab(VaadinIcon.BOLT.create());
            searchAndFilterTab = new Tab(VaadinIcon.SEARCH.create());
            optionsTab = new Tab(VaadinIcon.COG.create());
        }

        addSelectedChangeListener(event -> {
            Tab openedTab = event.getSelectedTab();
            if (openedTab.equals(quickCreateTab)) {
                controller.activeTaskProvider(quickCreateField);
            } else if (openedTab.equals(createNewTaskTab)) {
                controller.activeTaskProvider(createTaskForm);
            } else {
                controller.activeTaskProvider(null);
            }
        });

        return this;
    }

    public void initAll() {
        initCreateNewTaskTab();
        initSearchAndFilterTab();
        initOptionsTab();
        initQuickCreateTab();
    }

    private void initCloseTab() {
        closeTab = mobile.get()
                ? new Tab(VaadinIcon.CLOSE_SMALL.create())
                : new Tab(VaadinIcon.CLOSE.create());
        add(closeTab, new Div());
    }

    public ToolbarTabSheet initCreateNewTaskTab() {
        this.createTaskForm = new TaskFormLayout(controller);
        createTaskForm.taskBinder().setBean(new Task(null));

        createTaskForm.onClear(() -> {
            createTaskForm.taskBinder().setBean(new Task(null));
            createTaskForm.nodeBinder().setBean(new TaskNodeInfo());
        });
        createTaskForm.onSave(() -> {
            controller.addTaskFromProvider(createTaskForm);
            createTaskForm.taskBinder().setBean(new Task(null));
            createTaskForm.nodeBinder().setBean(new TaskNodeInfo());
        });

        createTaskForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        createNewTaskTab = mobile.get()
                ? new Tab("Create New Task")
                : new Tab(VaadinIcon.FILE_ADD.create());
        add(createNewTaskTab, this.createTaskForm);
        return this;
    }

    public ToolbarTabSheet initQuickCreateTab() {
        this.quickCreateField = new QuickCreateField(controller);
        quickCreateField.setWidthFull();

        quickCreateTab = mobile.get()
                ? new Tab(K.QUICK_CREATE)
                : new Tab(VaadinIcon.BOLT.create());

        add(quickCreateTab, this.quickCreateField);
        return this;
    }

    public ToolbarTabSheet initSearchAndFilterTab() {
        this.filterForm = new FilterForm(controller);
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        searchAndFilterTab = mobile.get()
                ? new Tab("Search & Filter")
                : new Tab(VaadinIcon.SEARCH.create());

        add(searchAndFilterTab, this.filterForm);
        return this;
    }


    public ToolbarTabSheet initOptionsTab() {
        Button recalculateTimeEstimates = new Button("Recalculate Time Estimates");
        recalculateTimeEstimates.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recalculateTimeEstimates.addClickListener(e -> controller.recalculateTimeEstimates());

        Checkbox disableContextMenu = new Checkbox("Disable Context Menu");
        disableContextMenu.setValue(!settings.enableContextMenu());
        disableContextMenu.addValueChangeListener(e -> {
            settings.enableContextMenu(!disableContextMenu.getValue());
            UI.getCurrent().getPage().reload();
        });

        this.options = new HorizontalLayout(
                recalculateTimeEstimates, disableContextMenu);
        options.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        optionsTab = mobile.get()
                ? new Tab("Options")
                : new Tab(VaadinIcon.COG.create());

        add(optionsTab, this.options);
        return this;
    }
}
