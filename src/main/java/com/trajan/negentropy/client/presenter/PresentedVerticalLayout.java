package com.trajan.negentropy.client.presenter;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.server.facade.TaskQueryService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

@UIScope
public class PresentedVerticalLayout extends VerticalLayout {
    @Autowired protected TaskQueryService queryService;
    @Autowired protected TreeViewPresenter presenter;
}
