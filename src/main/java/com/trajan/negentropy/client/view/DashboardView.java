package com.trajan.negentropy.client.view;

import com.trajan.negentropy.client.MainLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard | Vaadin CRM")
public class DashboardView extends VerticalLayout {
//    private final DataServiceImpl service;
//
//    public DashboardView(DataServiceImpl service) {
//        this.service = service;
//        addClassName("dashboard-view");
//        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
//        add(getTaskStats());
//    }

//    private Component getTaskStats() {
//        Span stats = new Span(service.countTasks() + " tasks");
//        stats.addClassNames(
//                LumoUtility.FontSize.XLARGE,
//                LumoUtility.Margin.Top.MEDIUM);
//        return stats;
//    }
}