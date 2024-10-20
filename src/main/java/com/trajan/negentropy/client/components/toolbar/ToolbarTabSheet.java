package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.RoutineView;
import com.trajan.negentropy.client.TreeView;
import com.trajan.negentropy.client.components.quickcreate.QuickCreateField;
import com.trajan.negentropy.client.components.routinelimit.RoutineSelect;
import com.trajan.negentropy.client.components.routinelimit.RoutineSelect.SelectOptions;
import com.trajan.negentropy.client.components.routinelimit.StartRoutineDialog;
import com.trajan.negentropy.client.components.searchandfilterform.AbstractSearchAndFilterForm;
import com.trajan.negentropy.client.components.searchandfilterform.TaskFilterForm;
import com.trajan.negentropy.client.components.searchandfilterform.TaskNodeFilterForm;
import com.trajan.negentropy.client.components.searchandfilterform.TreeFilterForm;
import com.trajan.negentropy.client.components.taskform.TaskNodeInfoFormFullLayout;
import com.trajan.negentropy.client.components.taskform.TaskNodeInfoFormMinorLayout;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.InsertMode;
import com.trajan.negentropy.client.controller.util.OnSuccessfulSaveActions;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter.NestableTaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.InsertRoutineStepChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.TabSheetVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SpringComponent
@Scope("prototype")
public class ToolbarTabSheet extends TabSheet {
    UILogger log = new UILogger();

    @Autowired private UIController controller;
    @Autowired private UserSettings settings;

    private TreeView treeView;
    private RoutineView routineView;

    @Getter private Tab closeTab;
    @Getter private Tab quickCreateTab;
    @Getter private Tab searchAndFilterTab;
    @Getter private TaskNodeFilterForm filterForm;
    @Getter private TaskProviderTab createNewTaskTab;
    @Getter private Tab optionsTab;
    @Getter private Tab insertTaskTab;
    @Getter private Tab startRoutineTab;
    @Getter private Tab hideRoutineStepsTab;
    @Getter private Tab showRoutineStepsTab;
    @Getter private Tab addStepToRoutineTab;

    public enum TabType {
        CLOSE_TAB,
        CREATE_NEW_TASK_TAB_FULL,
        CREATE_NEW_TASK_TAB_MINOR,
        INSERT_TASK_TAB,
        START_ROUTINE_TAB,
        SEARCH_AND_FILTER_TAB,
        OPTIONS_TAB,
        QUICK_CREATE_TAB,
        HIDE_ROUTINE_STEPS_TAB,
        SHOW_ROUTINE_STEPS_TAB,
        ADD_STEP_TAB
    }

    private TaskNodeInfoFormMinorLayout createTaskForm;
    private QuickCreateField quickCreateField;
    private TaskListBox taskSetBox;

    private static final String HIDE_ROUTINE_INFO_BARS = "hide-routine-info-bars";
    public void init(TreeView treeView, TabType... tabsNames) {
        this.treeView = treeView;

        init(() -> {}, tabsNames);
    }

    public void init(RoutineView routineView, Runnable onCloseClick, TabType... tabsNames) {
        this.routineView = routineView;

        init(onCloseClick, tabsNames);
    }

    private void init(Runnable onCloseClick, TabType... tabsNames) {
        UI.getCurrent().getPage().retrieveExtendedClientDetails(receiver -> {
            boolean mobile = (receiver.getWindowInnerWidth() > K.BREAKPOINT_PX);

            for (TabType tabName : tabsNames) {
                switch (tabName) {
                    case CLOSE_TAB -> initCloseTab(mobile);
                    case CREATE_NEW_TASK_TAB_FULL -> initCreateNewTaskTab(mobile, true);
                    case CREATE_NEW_TASK_TAB_MINOR -> initCreateNewTaskTab(mobile, false);
                    case INSERT_TASK_TAB -> initInsertTaskTab(mobile);
                    case START_ROUTINE_TAB -> initStartRoutineFromTaskTab(mobile);
                    case SEARCH_AND_FILTER_TAB -> initSearchAndFilterTab(mobile);
                    case OPTIONS_TAB -> initOptionsTab(mobile);
                    case QUICK_CREATE_TAB -> initQuickCreateTab(mobile);
                    case HIDE_ROUTINE_STEPS_TAB -> initHideRoutineStepsTab(mobile);
                    case SHOW_ROUTINE_STEPS_TAB -> initShowRoutineStepsTab(mobile);
                    case ADD_STEP_TAB -> initAddStepToRoutineTab(mobile);
                }
            }

            addSelectedChangeListener(event -> {
                Tab tab = event.getSelectedTab();
                if (tab instanceof TaskProviderTab taskProviderTab) {
                    controller.activeTaskNodeProvider(taskProviderTab.taskNodeProvider());
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

    private ToolbarTabSheet initCreateNewTaskTab(boolean mobile, boolean full) {
        this.createTaskForm = (full)
                ? new TaskNodeInfoFormFullLayout(controller)
                : new TaskNodeInfoFormMinorLayout(controller);

        createTaskForm.taskBinder().setBean(new Task());

        createTaskForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        createNewTaskTab = mobile
                ? new TaskProviderTab(createTaskForm, "Create New Task")
                : new TaskProviderTab(createTaskForm, VaadinIcon.FILE_ADD.create());

        createNewTaskTab.addEnabledStateChangeListener(e -> {
            if (!e.newEnabledState()) {
                createTaskForm.clear();
            }
        });

        Button showAndHide = new Button(VaadinIcon.CHEVRON_UP.create());
        showAndHide.setWidthFull();
        showAndHide.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        showAndHide.addClickListener(e -> {
            boolean visible = !createTaskForm.isVisible();
            createTaskForm.setVisible(visible);
            showAndHide.setIcon((visible)
                    ? VaadinIcon.CHEVRON_UP.create()
                    : VaadinIcon.CHEVRON_DOWN.create());
        });

        createTaskForm.onClose(() -> {
            this.setSelectedTab(closeTab);
            if (createTaskForm.isVisible()
                    && createTaskForm.onSaveSelect().getValue().equals(OnSuccessfulSaveActions.CLOSE.toString())) {
                showAndHide.click();
            }
        });

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
        filterForm = new TreeFilterForm(controller);
        filterForm.name().setPlaceholder("Filter by Task Name");
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        searchAndFilterTab = mobile
                ? new Tab("Search & Filter")
                : new Tab(VaadinIcon.SEARCH.create());

        this.setSearchAndFilterTabBackgroundColor();
        filterForm.binder().addValueChangeListener(event -> setSearchAndFilterTabBackgroundColor());

        add(searchAndFilterTab, filterForm);
        return this;
    }

    private void setSearchAndFilterTabBackgroundColor() {
        NestableTaskNodeTreeFilter filter = filterForm.binder().getBean();
        if (filter.options().equals(Set.of()) &&
                filter.name().isBlank() &&
                (filter.completed() != null && !filter.completed()) &&
                filter.includedTagIds().equals(Set.of()) &&
                filter.excludedTagIds().equals(Set.of())) {
            searchAndFilterTab.removeClassName(K.BACKGROUND_COLOR_PRIMARY);
        } else {
            searchAndFilterTab.addClassName(K.BACKGROUND_COLOR_PRIMARY);
        }
    }

    private ToolbarTabSheet initOptionsTab(boolean mobile) {
        MenuBar optionsMenu = new MenuBar();
        Span optionsMenuText = new Span("Additional Options");
        optionsMenuText.add(LumoIcon.DROPDOWN.create());
        MenuItem additionalOptions = optionsMenu.addItem(optionsMenuText);
        SubMenu additionalOptionsSubmenu = additionalOptions.getSubMenu();

//        additionalOptionsSubmenu.addItem("Recalculate Net Durations",
//                e -> controller.recalculateNetDurations());

        additionalOptionsSubmenu.addItem("Delete All Orphan Tasks",
                e -> controller.deleteAllOrphanedTasks());

        additionalOptionsSubmenu.addItem("Force Data Resync",
                e -> {
            controller.taskNetworkGraph().reset();
            controller.taskEntryDataProvider().refreshAll();
        });

        additionalOptionsSubmenu.addItem("Delete All Completed Tasks",
                e -> controller.deleteAllCompletedTaskNodes());

//        RadioButtonGroup<String> gridTilingRadioButtonGroup = new RadioButtonGroup<>("Additional Grid View");
//        gridTilingRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
//        gridTilingRadioButtonGroup.setItems(Arrays.stream(GridTiling.values())
//                .map(GridTiling::value)
//                .toList());
//        gridTilingRadioButtonGroup.setValue(settings.gridTiling().value());
//        gridTilingRadioButtonGroup.addValueChangeListener(event ->
//            treeView.setGridTiling(GridTiling.get(event.getValue()).orElseThrow()));

        RadioButtonGroup<String> sameGridDragInsertModeRadioButtonGroup = new RadioButtonGroup<>("Drag Inside Grid");
        sameGridDragInsertModeRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        sameGridDragInsertModeRadioButtonGroup.setItems(Arrays.stream(InsertMode.values())
                .map(InsertMode::toString)
                .toList());
        sameGridDragInsertModeRadioButtonGroup.setValue(settings.sameGridDragInsertMode().toString());
        sameGridDragInsertModeRadioButtonGroup.addValueChangeListener(event ->
                settings.sameGridDragInsertMode(
                        InsertMode.get(event.getValue()).orElseThrow()));

//        RadioButtonGroup<String> betweenGridsDragInsertModeRadioButtonGroup = new RadioButtonGroup<>("Drag Between Grids");
//        betweenGridsDragInsertModeRadioButtonGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
//        betweenGridsDragInsertModeRadioButtonGroup.setItems(Arrays.stream(InsertMode.values())
//                .map(InsertMode::toString)
//                .toList());
//        betweenGridsDragInsertModeRadioButtonGroup.setValue(settings.differentGridDragInsertMode().toString());
//        betweenGridsDragInsertModeRadioButtonGroup.addValueChangeListener(event ->
//                settings.differentGridDragInsertMode(
//                        InsertMode.get(event.getValue()).orElseThrow()));

        Checkbox disableContextMenuCheckbox = new Checkbox("Disable Context Menu");
        disableContextMenuCheckbox.setValue(!settings.enableContextMenu());
        disableContextMenuCheckbox.addValueChangeListener(e -> {
            settings.enableContextMenu(!disableContextMenuCheckbox.getValue());
            UI.getCurrent().getPage().reload();
        });

        Checkbox autoRefreshCheckbox = new Checkbox("Auto-refresh Routines");
        RoutineService routineService = controller.services().routine();
        autoRefreshCheckbox.setValue(routineService.refreshRoutines());
        autoRefreshCheckbox.addValueChangeListener(event -> {
            log.debug("Toggling auto-refresh to " + !routineService.refreshRoutines());
            routineService.refreshRoutines(!routineService.refreshRoutines());
            NotificationMessage.result("Auto-refresh " + (!routineService.refreshRoutines() ? "disabled" : "enabled"));
            if (routineService.refreshRoutines()) CompletableFuture.runAsync(routineService::refreshActiveRoutines);
        });

        Checkbox hideRoutineTaskBars = new Checkbox("Hide routine ancestor bars");
        hideRoutineTaskBars.setValue(settings.hideRoutineTaskBars());
        hideRoutineTaskBars.addValueChangeListener(e -> {
            settings.hideRoutineTaskBars(hideRoutineTaskBars.getValue());
            Component view = UI.getCurrent().getCurrentView();
            if (hideRoutineTaskBars.getValue()) {
                view.addClassName(HIDE_ROUTINE_INFO_BARS);
                UI.getCurrent().getPage().reload();
            } else {
                view.removeClassName(HIDE_ROUTINE_INFO_BARS);
            }
            // TODO: Get CSS working, remove this
        });

        Checkbox hideFinishedRoutineSteps = new Checkbox("Hide inapplicable routine steps");
        hideFinishedRoutineSteps.setValue(settings.hideFinishedRoutineSteps());
        hideFinishedRoutineSteps.addValueChangeListener(e -> {
            settings.hideFinishedRoutineSteps(hideFinishedRoutineSteps.getValue());
            if (routineView != null) {
                routineView.routineStepTreeGrid().setRoutine(routineView.routineStepTreeGrid().routine());
            }
        });

        VerticalLayout auxiliaryButtonLayout = new VerticalLayout(hideFinishedRoutineSteps, hideRoutineTaskBars, disableContextMenuCheckbox, autoRefreshCheckbox, optionsMenu);
        auxiliaryButtonLayout.setPadding(false);
        auxiliaryButtonLayout.setSpacing(false);

        FormLayout layout = new FormLayout(sameGridDragInsertModeRadioButtonGroup, auxiliaryButtonLayout);
//                sameGridDragInsertModeRadioButtonGroup, betweenGridsDragInsertModeRadioButtonGroup,
//                gridTilingRadioButtonGroup, auxiliaryButtonLayout);

        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2),
                new FormLayout.ResponsiveStep(K.SHORT_SCREEN_WIDTH, 4));

        optionsTab = mobile
                ? new Tab("Options")
                : new Tab(VaadinIcon.COG.create());

        add(optionsTab, layout);
        return this;
    }

    public TaskNodeFilterForm createTaskNodeFilterForm() {
        TaskNodeFilterForm filterForm = new TaskNodeFilterForm(controller);
        filterForm.binder().setBean(new NestableTaskNodeTreeFilter());
        configureFilterForm(filterForm);
        return filterForm;
    }

    public TaskFilterForm createTaskFilterForm() {
        TaskFilterForm filterForm = new TaskFilterForm(controller);
        filterForm.binder().setBean(new TaskTreeFilter());
        configureFilterForm(filterForm);
        return filterForm;
    }

    private void configureFilterForm(AbstractSearchAndFilterForm filterForm) {
        filterForm.name().setPlaceholder("Search for individual task");
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);
        filterForm.name().setValueChangeMode(ValueChangeMode.TIMEOUT);
    }

    public TaskListBox createTaskSetBox(AbstractSearchAndFilterForm form) {
        taskSetBox = new TaskListBox(controller, form);
        taskSetBox.setWidthFull();
        taskSetBox.addClassNames(LumoUtility.Padding.NONE, LumoUtility.BoxSizing.BORDER);
        return taskSetBox;
    }

    private void configureTaskSearchProvider(AbstractSearchAndFilterForm filterForm, TaskListBox taskSetBox) {
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
        TaskFilterForm filterForm = createTaskFilterForm();
        filterForm.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        taskSetBox = createTaskSetBox(filterForm);

        insertTaskTab = mobile
                ? new TaskProviderTab(taskSetBox, "Insert Task")
                : new TaskProviderTab(taskSetBox, VaadinIcon.FILE_SEARCH.create());

        configureTaskSearchProvider(filterForm, taskSetBox);

        filterForm.goToCreateNewTaskFormButton().addClickListener(e -> {
            if (this.createTaskForm != null && this.createNewTaskTab != null) {
                String current = filterForm.name().getValue();
                this.setSelectedTab(createNewTaskTab);
                createTaskForm.clear();
                createTaskForm.nameField().setValue(current);
            } else {
                log.error("Create New Task Form or Tab is not initialized");
            }
        });

        VerticalLayout layout = new VerticalLayout(filterForm, taskSetBox);
        layout.addClassNames(LumoUtility.Padding.Horizontal.NONE, LumoUtility.Padding.Vertical.NONE,
                LumoUtility.BoxSizing.BORDER);

        add(insertTaskTab, layout);
        return this;
    }

    private ToolbarTabSheet initStartRoutineFromTaskTab(boolean mobile) {
        TaskNodeFilterForm filterForm = createTaskNodeFilterForm();
        filterForm.nested().setValue(false);
        filterForm.nested().setVisible(false);
        filterForm.binder().getBean().completed(false);
        taskSetBox = createTaskSetBox(filterForm);

        taskSetBox.addSelectionListener(e -> taskSetBox.getSelectedItems().stream().findFirst()
                .ifPresent(task -> {
                    StartRoutineDialog dialog = SpringContext.getBean(StartRoutineDialog.class);
                    taskSetBox.deselectAll();
                    dialog.open(List.of(task));
        }));

        startRoutineTab = mobile
                ? new TaskProviderTab(taskSetBox, "Start Routine")
                : new TaskProviderTab(taskSetBox, VaadinIcon.FIRE.create());

        configureTaskSearchProvider(filterForm, taskSetBox);

        RoutineSelect routineSelect = SpringContext.getBean(RoutineSelect.class);
        routineSelect.setValue(SelectOptions.NO_FILTER);

        VerticalLayout layout = new VerticalLayout(filterForm, taskSetBox);
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

    private ToolbarTabSheet initAddStepToRoutineTab(boolean mobile) {
        addStepToRoutineTab = mobile
                ? new Tab("Add Step")
                : new Tab(VaadinIcon.PLUS.create());

        ComboBox<Task> addStepComboBox = new ComboBox<>();
        addStepComboBox.setClassName("add-step-combo-box");
        addStepComboBox.setLabel("Add a step to an existing routine, or start a routine from a new step.");
        addStepComboBox.setItems(controller.taskNetworkGraph().taskMap().values());
        addStepComboBox.setItemLabelGenerator(Task::name);

//        addStepComboBox.setAllowCustomValue(true);
//        addStepComboBox.addCustomValueSetListener(event -> {
//            Task task = new Task();
//            task.name(event.getDetail());
//            addStepComboBox.setValue(task);
//        });

        Button addStepButton = new Button("Add Step");
        addStepButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addStepButton.addClickListener(event -> addStepToRoutineSave(addStepComboBox));

        Shortcuts.addShortcutListener(addStepComboBox,
                event -> addStepToRoutineSave(addStepComboBox), Key.ENTER);

        HorizontalLayout layout = new HorizontalLayout(addStepButton, addStepComboBox);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setAlignItems(Alignment.CENTER);

        add(addStepToRoutineTab, layout);
        return this;
    }

    private void addStepToRoutineSave(ComboBox<Task> addStepComboBox) {
        Task task = addStepComboBox.getValue();
        if (task != null) {
            Routine activeRoutine = routineView != null ? routineView.getActiveRoutine() : null;
            if (activeRoutine != null) {
                Change insertRoutineStepChange = new InsertRoutineStepChange(activeRoutine.id(), task);
                controller.requestChange(insertRoutineStepChange);
            } else {
                if (task.id() == null) {
                    Change persistTask = new PersistChange<>(task);
                    DataMapResponse response = controller.requestChange(persistTask);

                    if (response.success()) {
                        task = (Task) response.changeRelevantDataMap().getFirst(persistTask.id());
                    }
                }
                controller.createRoutine(task,
                        r -> addStepComboBox.clear(),
                        null);
            }
        }
    }

    public void setCreateTaskFormData(Task task, TaskNodeDTO data) {
        this.createTaskForm.taskBinder().setBean(task);
        this.createTaskForm.nodeInfoBinder().setBean(data);
        this.createTaskForm.onNameValueChange();
    }
}
