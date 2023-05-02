package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Negentropy")
@RouteAlias("tree")
@Accessors(fluent = true)
@Getter
public class TreeView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(TreeView.class);
    private final TreeViewPresenter presenter;

    private final TreeGridLayout gridLayout;
    private final TaskForm form;

    private final Label result;

    public TreeView(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.gridLayout = new TreeGridLayout(presenter);
        this.form = new TaskForm(presenter);
        this.result = new Label();

        this.addClassName("tree-view");
        this.setSizeFull();

        this.presenter.initTreeView(this);

        form.binder().setBean(new Task(null));

        configureLayout();
    }

    private void configureLayout() {
        Details formDetails = new Details("Create New Task", form);

        Details utilityDetails = new Details("Settings and Visibility");

        gridLayout.setHeightFull();
        gridLayout.setWidthFull();

        formDetails.setWidthFull();
        form.setWidthFull();

        this.add(
                formDetails,
                utilityDetails,
                result,
                gridLayout);
    }
}
