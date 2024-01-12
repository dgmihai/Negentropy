package com.trajan.negentropy.client;

import com.trajan.negentropy.client.components.appnav.AppNav;
import com.trajan.negentropy.client.components.appnav.AppNavItem;
import com.trajan.negentropy.client.components.taskform.TaskNodeInfoFormDialog;
import com.trajan.negentropy.client.components.wellness.MoodInput;
import com.trajan.negentropy.client.components.wellness.StressorInput;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.server.facade.response.RoutineResponse;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Right;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.util.function.Consumer;

public class MainLayout extends AppLayout {
    private final UILogger log = new UILogger();

    private H2 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        UI ui = UI.getCurrent();
        ui.getPage().setTitle("Negentropy");
        ui.getLoadingIndicatorConfiguration().setFirstDelay(100);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        Button pinnedTasks = new Button(VaadinIcon.PIN.create());
        pinnedTasks.addClassName(LumoUtility.FontSize.LARGE);
        pinnedTasks.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        pinnedTasks.addClickListener(event -> {
            Dialog pinnedTasksDialog = new Dialog();

            SessionServices services = SpringContext.getBean(SessionServices.class);

            pinnedTasksDialog.setHeaderTitle("Start Routine from Pinned Task");
            FormLayout taskButtonLayout = new FormLayout();
            taskButtonLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1),
                    new FormLayout.ResponsiveStep("25em", 2),
                    new FormLayout.ResponsiveStep("40em", 3));

            services.query().fetchAllTasks(new TaskTreeFilter()
                            .onlyPinned(true))
                    .forEach(task -> {
                        Button button = new Button(task.name());
                        button.addClickListener(e -> {
                            pinnedTasksDialog.close();

                            Consumer<RoutineResponse> onSuccess = response -> {
                                pinnedTasksDialog.close();
                                UI.getCurrent().navigate(RoutineView.class);
                            };
                            Consumer<RoutineResponse> onFailure = response -> {
                                NotificationMessage.error(response.message());
                            };

                            UIController controller = SpringContext.getBean(UIController.class);
                            controller.createRoutine(task, null, onSuccess, onFailure);
                        });

                        taskButtonLayout.add(button);
                    });
            pinnedTasksDialog.add(taskButtonLayout);
            pinnedTasksDialog.open();
        });

        Button addTask = new Button(VaadinIcon.PLUS.create());
        addTask.addClassName(LumoUtility.FontSize.LARGE);
        addTask.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        addTask.addClickListener(event -> {
            Dialog newTaskDialog = new Dialog();

            UIController controller = SpringContext.getBean(UIController.class);

            newTaskDialog.setHeaderTitle("Add Task");
            newTaskDialog.add(new TaskNodeInfoFormDialog(newTaskDialog, controller));
            newTaskDialog.open();
        });

        Button wellnessCheck = new Button(VaadinIcon.HEART.create());
        wellnessCheck.addClassName(LumoUtility.FontSize.LARGE);
        wellnessCheck.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        wellnessCheck.addClickListener(event -> {
            Dialog wellnessDialog = new Dialog();
            String title = "How are you doing?";

            try {
                SessionServices services = SpringContext.getBean(SessionServices.class);
                title = services.tenet().getRandom().toString();
            } catch (Exception e) {
                log.error("Failed to get random tenet for wellness dialog", e);
            }

            wellnessDialog.setHeaderTitle(title);
            wellnessDialog.add(
                    SpringContext.getBean(StressorInput.class),
                    SpringContext.getBean(MoodInput.class));
            wellnessDialog.setWidth("25rem");
            wellnessDialog.open();
        });

        Span buttonSpan = new Span();
        buttonSpan.add(pinnedTasks, wellnessCheck, addTask);
        buttonSpan.addClassNames(Left.AUTO, Right.SMALL);

        addToNavbar(false, toggle, viewTitle, buttonSpan);
    }

    private void addDrawerContent() {
        H1 appName = new H1("Negentropy");
        appName.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(header, scroller, createFooter());
    }

    private AppNav createNavigation() {
        // AppNav is not yet an official component.
        // For documentation, visit https://github.com/vaadin/vcf-nav#readme
        AppNav nav = new AppNav();

        nav.addItem(new AppNavItem("Task Tree", TreeView.class, LineAwesomeIcon.TREE_SOLID.create()));
        nav.addItem(new AppNavItem("Routines", RoutineView.class, LineAwesomeIcon.FIRE_ALT_SOLID.create()));
        nav.addItem(new AppNavItem("Records", RecordView.class, LineAwesomeIcon.BOOK_OPEN_SOLID.create()));
        nav.addItem(new AppNavItem("Tenets", TenetView.class, LineAwesomeIcon.COMPASS.create()));

//        nav.setWidth("5em");
        return nav;
    }

    private Footer createFooter() {
        return new Footer();
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}