//package com.trajan.negentropy.client;
//
//import com.trajan.negentropy.client.list.ListView;
//import com.trajan.negentropy.client.routine.RoutineView;
//import com.vaadin.flow.component.Component;
//import com.vaadin.flow.component.ComponentUtil;
//import com.vaadin.flow.component.applayout.AppLayout;
//import com.vaadin.flow.component.applayout.DrawerToggle;
//import com.vaadin.flow.component.html.H1;
//import com.vaadin.flow.component.orderedlayout.FlexComponent;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.tabs.Tab;
//import com.vaadin.flow.component.tabs.Tabs;
//import com.vaadin.flow.component.tabs.TabsVariant;
//import com.vaadin.flow.router.RouterLink;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import com.vaadin.flow.spring.annotation.UIScope;
//import com.vaadin.flow.theme.lumo.LumoUtility;
//
//@UIScope
//@SpringComponent
//public class MainLayout extends AppLayout {
//
//    public MainLayout() {
//        createHeader();
//        createDrawer();
//    }
//
//    private void createHeader() {
//        H1 logo = new H1("NEGENTROPY");
//        logo.addClassNames(
//                LumoUtility.FontSize.SMALL,
//                LumoUtility.Margin.SMALL);
//
//        var header = new HorizontalLayout(new DrawerToggle(), logo );
//
//        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
//        header.setWidthFull();
//        header.addClassNames(
//                LumoUtility.Padding.Vertical.NONE,
//                LumoUtility.Padding.Horizontal.MEDIUM);
//        header.add(createMenu());
//
//        addToNavbar(header);
//    }
//
//    private void createDrawer() {
//        addToDrawer(new VerticalLayout(
//                new RouterLink("List", ListView.class),
//                new RouterLink("Routine", RoutineView.class)
//        ));
//    }
//
//    private Tabs createMenu() {
//        final Tabs tabs = new Tabs();
//        tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
//        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
//        tabs.setId("tabs");
//        //tabs.add(createMenuItems());
//        return tabs;
//    }
//
//    private static Tab createTab(String text,
//                                 Class<? extends Component> navigationTarget) {
//        final Tab tab = new Tab();
//        tab.add(new RouterLink(text, navigationTarget));
//        ComponentUtil.setData(tab, Class.class, navigationTarget);
//        return tab;
//    }
//}