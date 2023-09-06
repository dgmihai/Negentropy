package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.components.filterform.FilterForm;
import com.trajan.negentropy.client.components.filterform.TreeFilterForm;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.taskform.TaskNodeInfoFormLayout;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.session.enums.GridTiling;
import com.trajan.negentropy.client.tree.TreeView;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
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

import java.util.Arrays;
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
    @Getter private Tab hideRoutineStepsTab;
    @Getter private Tab showRoutineStepsTab;

    public enum TabType {
        CLOSE_TAB,
        CREATE_NEW_TASK_TAB,
        INSERT_TASK_TAB,
        START_ROUTINE_TAB,
        SEARCH_AND_FILTER_TAB,
        OPTIONS_TAB,
        QUICK_CREATE_TAB,
        HIDE_ROUTINE_STEPS_TAB,
        SHOW_ROUTINE_STEPS_TAB
    }

    private TaskNodeInfoFormLayout createTaskForm;
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
                    case HIDE_ROUTINE_STEPS_TAB -> initHideRoutineStepsTab(mobile);
                    case SHOW_ROUTINE_STEPS_TAB -> initShowRoutineStepsTab(mobile);
                }
            }

            addSelectedChangeListener(event -> {
                Tab tab = event.getSelectedTab();
                if (tab instanceof TaskProviderTab taskProviderTab) {
                    controller.activeTaskNodeProvider(taskProviderTab.taskProvider());
                } else {
                    controller.activeTaskNodeProvider(null);
                }
            });

            this.setWidthFull();
            this.addThemeVariants(TabSheetVariant.LUMO_TABS_CENTERED);

            if (closeTab != null) {
                closeTab.getElement().addEventListener("click", e -> onCloseClick.run());
            }
        });
    }

    private void initCloseTab(boolean mobile) {
        closeTab = mobile
                ? new Tab(VaadinIcon.CLOSE_SMALL.create())
                : new Tab(VaadinIcon.CLOSE.create());
        add(closeTab, new Div());
    }

    private ToolbarTabSheet initCreateNewTaskTab(boolean mobile) {
        this.createTaskForm = new TaskNodeInfoFormLayout(controller);
        createTaskForm.taskBinder().setBean(new Task());

        createTaskForm.afterClear(() -> {
            createTaskForm.taskBinder().setBean(new Task());
            createTaskForm.nodeInfoBinder().setBean(new TaskNodeDTO());
        });
        createTaskForm.onClose(() -> this.setSelectedTab(closeTab));

        createTaskForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        createNewTaskTab = mobile
                ? new TaskProviderTab(createTaskForm, "Create New Task")
                : new TaskProviderTab(createTaskForm, VaadinIcon.FILE_ADD.create());

        Button showAndHide = new Button(VaadinIcon.CHEVRON_UP.create());
        showAndHide.setWidthFull();
        showAndHide.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        showAndHide.addClickListener(e -> createTaskForm.setVisible(!createTaskForm.isVisible()));
        VerticalLayout layout = new VerticalLayout(createTaskForm, showAndHide);

        add(createNewTaskTab, layout);
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
        Button recalculateNetDurations = new Button("Recalculate Net Durations");
        recalculateNetDurations.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        recalculateNetDurations.addClickListener(e -> controller.recalculateNetDurations());

        Button removeOrphanTasks = new Button("Delete All Orphan Tasks");
        removeOrphanTasks.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        removeOrphanTasks.addClickListener(e -> controller.deleteAllOrphanedTasks());

        Button forceResync = new Button("Force Data Resync");
        forceResync.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        forceResync.addClickListener(e -> {
            controller.taskNetworkGraph().reset();
            controller.taskEntryDataProviderManager().refreshAllProviders();
        });

        Button deleteCompleted = new Button("Delete All Completed Tasks");
        deleteCompleted.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteCompleted.addClickListener(e -> controller.deleteAllCompletedTasks());

        RadioButtonGroup<String> gridTilingRadioButtonGroup = new RadioButtonGroup<>("Additional Grid View");
        gridTilingRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        gridTilingRadioButtonGroup.setItems(Arrays.stream(GridTiling.values())
                .map(GridTiling::value)
                .toList());
        gridTilingRadioButtonGroup.setValue(settings.gridTiling().value());
        gridTilingRadioButtonGroup.addValueChangeListener(event ->
            treeView.setGridTiling(GridTiling.get(event.getValue()).orElseThrow()));

        RadioButtonGroup<String> sameGridDragInsertModeRadioButtonGroup = new RadioButtonGroup<>("Drag Inside Grid");
        sameGridDragInsertModeRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        sameGridDragInsertModeRadioButtonGroup.setItems(Arrays.stream(InsertMode.values())
                .map(InsertMode::toString)
                .toList());
        sameGridDragInsertModeRadioButtonGroup.setValue(settings.sameGridDragInsertMode().toString());
        sameGridDragInsertModeRadioButtonGroup.addValueChangeListener(event ->
                settings.sameGridDragInsertMode(
                        InsertMode.get(event.getValue()).orElseThrow()));

        RadioButtonGroup<String> betweenGridsDragInsertModeRadioButtonGroup = new RadioButtonGroup<>("Drag Between Grids");
        betweenGridsDragInsertModeRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        betweenGridsDragInsertModeRadioButtonGroup.setItems(Arrays.stream(InsertMode.values())
                .map(InsertMode::toString)
                .toList());
        betweenGridsDragInsertModeRadioButtonGroup.setValue(settings.differentGridDragInsertMode().toString());
        betweenGridsDragInsertModeRadioButtonGroup.addValueChangeListener(event ->
                settings.differentGridDragInsertMode(
                        InsertMode.get(event.getValue()).orElseThrow()));

        Checkbox disableContextMenu = new Checkbox("Disable Context Menu");
        disableContextMenu.setValue(!settings.enableContextMenu());
        disableContextMenu.addValueChangeListener(e -> {
            settings.enableContextMenu(!disableContextMenu.getValue());
            UI.getCurrent().getPage().reload();
        });

        VerticalLayout auxiliaryButtonLayout = new VerticalLayout(disableContextMenu, recalculateNetDurations,
                removeOrphanTasks, forceResync, deleteCompleted);
        auxiliaryButtonLayout.setPadding(false);
        auxiliaryButtonLayout.setSpacing(false);

        FormLayout layout = new FormLayout(
                sameGridDragInsertModeRadioButtonGroup, betweenGridsDragInsertModeRadioButtonGroup,
                gridTilingRadioButtonGroup, auxiliaryButtonLayout);

        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2),
                new FormLayout.ResponsiveStep(K.SHORT_WIDTH, 4));

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
        filterForm.binder().setBean(new TaskTreeFilter());
        filterForm.name().setValueChangeMode(ValueChangeMode.TIMEOUT);
        return filterForm;
    }

    public TaskListBox createTaskSetBox(FilterForm form) {
        taskSetBox = new TaskListBox(controller, form);
        taskSetBox.setWidthFull();
        taskSetBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
        return taskSetBox;
    }

    private void configureTaskSearchProvider(FilterForm filterForm, TaskListBox taskSetBox) {
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
                boolean present = task.isPresent();
                filterForm.setVisible(!present);
                filterForm.setReadOnly(present);
                task.ifPresentOrElse(
                        taskSetBox::hideOtherTasks,
                        () -> {
                            taskSetBox.fetchTasks(filterForm.binder().getBean());
                        });
            }
        });
    }

    private ToolbarTabSheet initInsertTaskTab(boolean mobile) {
        FilterForm filterForm = createTaskFilterForm();
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        taskSetBox = createTaskSetBox(filterForm);

        insertTaskTab = mobile
                ? new TaskProviderTab(taskSetBox, "Insert Task")
                : new TaskProviderTab(taskSetBox, VaadinIcon.FILE_SEARCH.create());

        configureTaskSearchProvider(filterForm, taskSetBox);

        VerticalLayout layout = new VerticalLayout(filterForm, taskSetBox);
        layout.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.NONE,
                LumoUtility.BoxSizing.BORDER);

        add(insertTaskTab, layout);
        return this;
    }

    private ToolbarTabSheet initStartRoutineFromTaskTab(boolean mobile) {
        FilterForm filterForm = createTaskFilterForm();
        taskSetBox = createTaskSetBox(filterForm);

        startRoutineTab = mobile
                ? new TaskProviderTab(taskSetBox, "Start Routine")
                : new TaskProviderTab(taskSetBox, VaadinIcon.FIRE.create());

        configureTaskSearchProvider(filterForm, taskSetBox);

        Button startRoutineButton = new Button("Start Routine");
        startRoutineButton.addClickListener(event ->
                controller.createRoutine(taskSetBox.getTask()));

        VerticalLayout layout = new VerticalLayout(filterForm, taskSetBox, startRoutineButton);
        layout.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        add(startRoutineTab, layout);
        return this;
    }

    private ToolbarTabSheet initShowRoutineStepsTab(boolean mobile) {
        showRoutineStepsTab = mobile
                ? new Tab("Show Routine Steps")
                : new Tab(VaadinIcon.EYE.create());
        add(showRoutineStepsTab, new Div());
        return this;
    }

    private ToolbarTabSheet initHideRoutineStepsTab(boolean mobile) {
        hideRoutineStepsTab = mobile
                ? new Tab("Hide Routine Steps")
                : new Tab(VaadinIcon.EYE_SLASH.create());
        add(hideRoutineStepsTab, new Div());
        return this;
    }
}
