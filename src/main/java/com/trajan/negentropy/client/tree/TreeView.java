package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.quickadd.QuickAddField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.tree.components.FilterLayout;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PageTitle("Negentropy - Task Tree")
@Route(value = "", layout = MainLayout.class)
@Uses(Icon.class)
@RouteAlias("tree")
@Accessors(fluent = true)
@Getter
public class TreeView extends Div {
    private static final Logger logger = LoggerFactory.getLogger(TreeView.class);
    private final TreeViewPresenter presenter;

    private final QuickAddField quickAddField;
    private final FilterLayout filterDiv;
    private final TaskTreeGrid taskTreeGrid;
    private final TaskFormLayout form;

    public TreeView(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.taskTreeGrid = new TaskTreeGrid(presenter);
        this.presenter.initTreeView(this);

        this.addClassName("tree-view");
        this.setSizeFull();

        TabSheet tabSheet = new TabSheet();

        this.quickAddField = new QuickAddField(presenter::onQuickAdd);
        quickAddField.setWidthFull();

        this.filterDiv = new FilterLayout(presenter);

        this.form = new TaskFormLayout(presenter, new Task(null));
        form.binder().setBean(new Task(null));

        form.onClear(() -> {
            form.binder().setBean(new Task(null));
            tabSheet.setSelectedIndex(0);
        });
        form.onSave(() -> {
            presenter.onTaskFormSave(form);
            form.binder().setBean(new Task(null));
        });
        form.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.XSMALL,
                LumoUtility.BoxSizing.BORDER);

        Button recalculateTimeEstimates = new Button("Recalculate Time Estimates");
        recalculateTimeEstimates.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        recalculateTimeEstimates.addClickListener(e -> presenter.recalculateTimeEstimates());

        HorizontalLayout options = new HorizontalLayout(recalculateTimeEstimates);

        tabSheet.setWidthFull();
        tabSheet.add(VaadinIcon.CLOSE_SMALL.create(), new Div());
        tabSheet.add("Quick Add", quickAddField);
        tabSheet.add("Search & Filter", filterDiv());
        tabSheet.add("Create New Task", form);
        tabSheet.add("Options", options);

        taskTreeGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(
                tabSheet,
                taskTreeGrid);

        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
    }
}
