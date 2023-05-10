package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.client.components.quickadd.QuickAddField;
import com.trajan.negentropy.client.components.taskform.TaskFormLayout;
import com.trajan.negentropy.client.tree.components.FilterLayout;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
    private final TaskTreeGrid gridLayout;
    private final TaskFormLayout form;

    public TreeView(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.gridLayout = new TaskTreeGrid(presenter);
        this.presenter.initTreeView(this);

        this.addClassName("tree-view");
        this.setSizeFull();

        this.quickAddField = new QuickAddField(presenter::onQuickAdd);
        quickAddField.setWidthFull();

        gridLayout.setSizeFull();

        this.filterDiv = new FilterLayout(presenter);
        Details filterDetails = new Details("Filter", filterDiv);
        filterDetails.addThemeVariants(DetailsVariant.SMALL);
        filterDetails.setWidthFull();

        this.form = new TaskFormLayout(presenter, new Task(null));
        form.binder().setBean(new Task(null));
        Details formDetails = new Details("Create New Task", form);
        formDetails.addThemeVariants(DetailsVariant.SMALL);
        form.onClear(() -> {
            form.binder().setBean(new Task(null));
            formDetails.setOpened(false);
        });
        form.onSave(() -> {
            presenter.onTaskFormSave(form);
            form.binder().setBean(new Task(null));
        });
        form.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxSizing.BORDER);
        formDetails.setWidthFull();

        VerticalLayout layout = new VerticalLayout(
                quickAddField,
                filterDetails,
                formDetails,
                gridLayout);

        layout.setSizeFull();
        layout.setSpacing(false);
        this.add(layout);
    }
}
