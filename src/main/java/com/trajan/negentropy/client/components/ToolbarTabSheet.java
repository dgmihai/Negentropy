package com.trajan.negentropy.client.components;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
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
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

@SpringComponent
@Scope("prototype")
@Slf4j
public class ToolbarTabSheet extends TabSheet {
    @Autowired
    private ClientDataController controller;
    @Autowired
    private UserSettings settings;

    @Getter
    private Tab closeTab;
    @Getter
    private Tab quickCreateTab;
    @Getter
    private Tab searchAndFilterTab;
    @Getter
    private Tab createNewTaskTab;
    @Getter
    private Tab optionsTab;

    public enum TabType {
        CLOSE_TAB,
        CREATE_NEW_TASK_TAB,
        SEARCH_AND_FILTER_TAB,
        OPTIONS_TAB,
        QUICK_CREATE_TAB,
    }

    private TaskFormLayout createTaskForm;
    private QuickCreateField quickCreateField;
    private FilterForm filterForm;
    private HorizontalLayout options;

    public void initWithAllTabs() {
        this.init(TabType.values());
    }

    public void init(TabType... tabsNames) {
        init(() -> {}, tabsNames);
    }

    public void init(Runnable onCloseClick, TabType... tabsNames) {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver -> {
            boolean mobile = (receiver.getWindowInnerWidth() > K.BREAKPOINT_PX);

            for (TabType tabName : tabsNames) {
                switch (tabName) {
                    case CLOSE_TAB -> initCloseTab(mobile);
                    case CREATE_NEW_TASK_TAB -> initCreateNewTaskTab(mobile);
                    case SEARCH_AND_FILTER_TAB -> initSearchAndFilterTab(mobile);
                    case OPTIONS_TAB -> initOptionsTab(mobile);
                    case QUICK_CREATE_TAB -> initQuickCreateTab(mobile);
                }
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

            this.setWidthFull();
            this.addThemeVariants(TabSheetVariant.LUMO_TABS_CENTERED);

            closeTab.getElement().addEventListener("click", e -> onCloseClick.run());
        });
    }

    private void initCloseTab(boolean mobile) {
        closeTab = mobile
                ? new Tab(VaadinIcon.CLOSE_SMALL.create())
                : new Tab(VaadinIcon.CLOSE.create());
        add(closeTab, new Div());
    }

    private ToolbarTabSheet initCreateNewTaskTab(boolean mobile) {
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

        createNewTaskTab = mobile
                ? new Tab("Create New Task")
                : new Tab(VaadinIcon.FILE_ADD.create());

        add(createNewTaskTab, this.createTaskForm);
        return this;
    }

    private ToolbarTabSheet initQuickCreateTab(boolean mobile) {
        this.quickCreateField = new QuickCreateField(controller);
        quickCreateField.setWidthFull();

        quickCreateTab = mobile
                ? new Tab(K.QUICK_CREATE)
                : new Tab(VaadinIcon.BOLT.create());

        add(quickCreateTab, this.quickCreateField);
        return this;
    }

    private ToolbarTabSheet initSearchAndFilterTab(boolean mobile) {
        this.filterForm = new FilterForm(controller);
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        searchAndFilterTab = mobile
                ? new Tab("Search & Filter")
                : new Tab(VaadinIcon.SEARCH.create());

        add(searchAndFilterTab, this.filterForm);
        return this;
    }

    private ToolbarTabSheet initOptionsTab(boolean mobile) {
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

        optionsTab = mobile
                ? new Tab("Options")
                : new Tab(VaadinIcon.COG.create());

        add(optionsTab, this.options);
        return this;
    }
}
