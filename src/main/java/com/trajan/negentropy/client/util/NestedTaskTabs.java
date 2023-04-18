package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.client.presenter.ClientPresenter;
import com.trajan.negentropy.client.view.ListView;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import java.util.Stack;


public class NestedTaskTabs extends Tabs {
    private final ClientPresenter presenter;
    private final ListView listView;

    public NestedTaskTabs(ClientPresenter presenter, ListView listView) {
        super();
        this.presenter = presenter;
        this.listView = listView;

        add(new Tab(new Icon(VaadinIcon.HOME)));

        this.addSelectedChangeListener(e -> setRootEntry());
    }

    private void setRootEntry() {
        int tabIndex = getSelectedIndex();
        if (getSelectedTab() instanceof TaskTab tab) {
            presenter.setRootEntry(tab.getEntry());
        } else presenter.setRootEntry(null);
        while (getComponentCount() > tabIndex + 1) {
            remove(getTabAt(tabIndex + 1));
        }
        //verifyEntryExists();
    }

    public void onSelectNewRootEntry(TaskEntry entry) {
        Stack<TaskEntry> stack = new Stack<>();
        stack.push(entry);
        TaskEntry current = entry;
        while (current.parent() != null || !current.equals(entry)) {
            current = current.parent();
            stack.push(current);
        }
        TaskTab tab = null;
        while (!stack.empty()) {
            tab = new TaskTab(stack.pop());
            this.add(tab);
        }
        setSelectedTab(tab);
    }

//    public void verifyEntryExists() {
//        if (presenter.getRootEntry() != null) {
//            this.setSelectedIndex(this.getSelectedIndex() - 1);
//            this.verifyEntryExists();
//        }
//    }
}