package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.filterform.FilterForm;
import com.trajan.negentropy.client.components.filterform.TreeFilterForm;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.tree.TreeView;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.Optional;

@SpringComponent
@Scope("prototype")
@Slf4j
public class ToolbarTabSheet extends TabSheet {
    @Autowired
    private ClientDataController controller;
    @Autowired
    private UserSettings settings;

    private TreeView treeView;

    @Getter private Tab closeTab;
    @Getter private Tab quickCreateTab;
    @Getter private Tab searchAndFilterTab;
    @Getter private Tab createNewTaskTab;
    @Getter private Tab optionsTab;
    @Getter private Tab insertTaskTab;
    @Getter private Tab startRoutineTab;

    public enum TabType {
        CLOSE_TAB,
        CREATE_NEW_TASK_TAB,
        INSERT_TASK_TAB,
        START_ROUTINE_TAB,
        SEARCH_AND_FILTER_TAB,
        OPTIONS_TAB,
        QUICK_CREATE_TAB,
    }

    private TaskFormLayout createTaskForm;
    private QuickCreateField quickCreateField;
    private TaskListBox taskSetBox;

    public void init(TreeView treeView, TabType... tabsNames) {
        this.treeView = treeView;
        init(() -> {}, tabsNames);
    }

    public void init(Runnable onCloseClick, TabType... tabsNames) {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver -> {
            boolean mobile = (receiver.getWindowInnerWidth() > K.BREAKPOINT_PX);

            for (TabType tabName : tabsNames) {
                switch (tabName) {
                    case CLOSE_TAB -> initCloseTab(mobile);
                    case CREATE_NEW_TASK_TAB -> initCreateNewTaskTab(mobile);
                    case INSERT_TASK_TAB -> initInsertTaskTab(mobile);
                    case START_ROUTINE_TAB -> initStartRoutineFromTaskTab(mobile);
                    case SEARCH_AND_FILTER_TAB -> initSearchAndFilterTab(mobile);
                    case OPTIONS_TAB -> initOptionsTab(mobile);
                    case QUICK_CREATE_TAB -> initQuickCreateTab(mobile);
                }
            }

            addSelectedChangeListener(event -> {
                if (event.getSelectedTab() instanceof TaskProviderTab taskProviderTab) {
                    controller.activeTaskProvider(taskProviderTab.taskProvider());
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
                ? new TaskProviderTab(createTaskForm, "Create New Task")
                : new TaskProviderTab(createTaskForm, VaadinIcon.FILE_ADD.create());

        add(createNewTaskTab, this.createTaskForm);
        return this;
    }

    private ToolbarTabSheet initQuickCreateTab(boolean mobile) {
        this.quickCreateField = new QuickCreateField(controller);
        quickCreateField.setWidthFull();

        quickCreateTab = mobile
                ? new TaskProviderTab(quickCreateField, K.QUICK_CREATE)
                : new TaskProviderTab(quickCreateField, VaadinIcon.BOLT.create());

        add(quickCreateTab, this.quickCreateField);
        return this;
    }

    private ToolbarTabSheet initSearchAndFilterTab(boolean mobile) {
        TreeFilterForm filterForm = new TreeFilterForm(controller);
        filterForm.name().setPlaceholder("Filter task grid");
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        searchAndFilterTab = mobile
                ? new Tab("Search & Filter")
                : new Tab(VaadinIcon.SEARCH.create());

        add(searchAndFilterTab, filterForm);
        return this;
    }

    private ToolbarTabSheet initOptionsTab(boolean mobile) {
        Button recalculateTimeEstimates = new Button("Recalculate Time Estimates");
        recalculateTimeEstimates.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recalculateTimeEstimates.addClickListener(e -> controller.recalculateTimeEstimates());

        RadioButtonGroup<String> gridTilingRadioButtonGroup = new RadioButtonGroup<>();
        gridTilingRadioButtonGroup.setItems("None", "Vertical", "Horizontal");
        gridTilingRadioButtonGroup.setValue(settings.gridTiling().value());
        gridTilingRadioButtonGroup.addValueChangeListener(event ->
            treeView.setGridTiling(UserSettings.GridTiling.get(event.getValue()).orElseThrow()));

        Checkbox disableContextMenu = new Checkbox("Disable Context Menu");
        disableContextMenu.setValue(!settings.enableContextMenu());
        disableContextMenu.addValueChangeListener(e -> {
            settings.enableContextMenu(!disableContextMenu.getValue());
            UI.getCurrent().getPage().reload();
        });

        HorizontalLayout layout = new HorizontalLayout(
                recalculateTimeEstimates, gridTilingRadioButtonGroup, disableContextMenu);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);

        optionsTab = mobile
                ? new Tab("Options")
                : new Tab(VaadinIcon.COG.create());

        add(optionsTab, layout);
        return this;
    }

    public FilterForm createTaskFilterForm() {
        FilterForm filterForm = new FilterForm(controller);
        filterForm.name().setPlaceholder("Search for individual task");
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);
        filterForm.binder().setBean(new TaskFilter());
        filterForm.name().setValueChangeMode(ValueChangeMode.TIMEOUT);
        return filterForm;
    }

    public TaskListBox createTaskSetBox(FilterForm form) {
        taskSetBox = new TaskListBox(controller);
        taskSetBox.setWidthFull();
        taskSetBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
        return taskSetBox;
    }

    private Component createTaskSearchProvider(FilterForm filterForm, TaskListBox taskSetBox) {
        taskSetBox.addValueChangeListener(event -> {
            Optional<Task> task = taskSetBox.getValue().stream().findFirst();
            task.ifPresent(t -> filterForm.binder().getBean().name(t.name()));
        });

        filterForm.binder().addValueChangeListener(event -> {
            if (filterForm.binder().isValid()) {
                taskSetBox.fetchTasks(filterForm.binder().getBean());
            }
        });

        taskSetBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                Optional<Task> task = taskSetBox.getValue().stream().findFirst();
                task.ifPresentOrElse(
                        t -> {
                            filterForm.setVisible(false);
                            taskSetBox.hideOtherTasks(t);
                        },
                        () -> {
                            filterForm.setVisible(true);
                            taskSetBox.fetchTasks(filterForm.binder().getBean());
                        });
            }
        });

        VerticalLayout layout = new VerticalLayout(filterForm, taskSetBox);
        layout.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);
        return layout;
    }

    private ToolbarTabSheet initInsertTaskTab(boolean mobile) {
        FilterForm filterForm = createTaskFilterForm();
        taskSetBox = createTaskSetBox(filterForm);

        insertTaskTab = mobile
                ? new TaskProviderTab(taskSetBox, "Insert Task")
                : new TaskProviderTab(taskSetBox, VaadinIcon.COPY_O.create());

        add(insertTaskTab, createTaskSearchProvider(filterForm, taskSetBox));
        return this;
    }

    private ToolbarTabSheet initStartRoutineFromTaskTab(boolean mobile) {
        FilterForm filterForm = createTaskFilterForm();
        taskSetBox = createTaskSetBox(filterForm);

        startRoutineTab = mobile
                ? new TaskProviderTab(taskSetBox, "Start Routine")
                : new TaskProviderTab(taskSetBox, VaadinIcon.FIRE.create());

         add(startRoutineTab, createTaskSearchProvider(filterForm, taskSetBox));
         return this;
    }
}
