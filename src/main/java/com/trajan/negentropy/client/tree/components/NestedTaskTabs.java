package com.trajan.negentropy.client.tree.components;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.Stack;

public class NestedTaskTabs extends Tabs {
    private final TreeViewPresenter presenter;
    private TaskEntry currentEntry;

    public NestedTaskTabs(TreeViewPresenter presenter) {
        super();
        this.presenter = presenter;

        this.add(new Tab(new Icon(VaadinIcon.HOME)));

        this.onSelectNewRootEntry(presenter.getBaseEntry());
        this.addSelectedChangeListener(e -> setRootEntry());

        this.addThemeVariants(TabsVariant.LUMO_SMALL);
    }

    private void setRootEntry() {
        int tabIndex = getSelectedIndex();
        if (getSelectedTab() instanceof TaskTab tab) {
            currentEntry = tab.getEntry();
            presenter.setBaseEntry(currentEntry);
        } else {
            currentEntry = null;
            presenter.setBaseEntry(null);
        }
        while (getComponentCount() > tabIndex + 1) {
            remove(getTabAt(tabIndex + 1));
        }
    }

    public void onSelectNewRootEntry(TaskEntry entry) {
        if(entry != null) {
            Stack<TaskEntry> stack = new Stack<>();
            TaskEntry current = entry;

            while (current != null && !current.equals(currentEntry)) {
                stack.push(current);
                current = current.parent();
            }

            TaskTab tab = null;
            while (!stack.empty()) {
                tab = new TaskTab(stack.pop());
                this.add(tab);
            }
            setSelectedTab(tab);
        }
    }
}