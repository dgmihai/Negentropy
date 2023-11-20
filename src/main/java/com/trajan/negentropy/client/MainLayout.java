package com.trajan.negentropy.client;

import com.trajan.negentropy.client.components.appnav.AppNav;
import com.trajan.negentropy.client.components.appnav.AppNavItem;
import com.trajan.negentropy.client.components.taskform.TaskNodeInfoFormDialog;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.logger.UILogger;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Left;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin.Right;
import org.vaadin.lineawesome.LineAwesomeIcon;

public class MainLayout extends AppLayout {
    private final UILogger log = new UILogger();

    private H2 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        UI.getCurrent().getPage().setTitle("Negentropy");
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.getElement().setAttribute("aria-label", "Menu toggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

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

        addTask.addClassNames(Left.AUTO, Right.SMALL);

        addToNavbar(false, toggle, viewTitle, addTask);
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