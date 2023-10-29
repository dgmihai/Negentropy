package com.trajan.negentropy.client.components.grid.subcomponents;

import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.UILogger;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.Stack;

public class NestedTaskTabs extends Tabs {
    private final UILogger log = new UILogger();

    private final TaskEntryTreeGrid taskEntryTreeGrid;
    private TaskEntry currentEntry;

    public NestedTaskTabs(TaskEntryTreeGrid taskEntryTreeGrid) {
        super();
        this.taskEntryTreeGrid = taskEntryTreeGrid;

        this.add(new Tab(new Icon(VaadinIcon.HOME)));

        this.onSelectNewRootEntry(null);
        this.addSelectedChangeListener(e -> setRootEntry());

        this.addThemeVariants(TabsVariant.LUMO_SMALL);
    }

    private void setRootEntry() {
        int tabIndex = getSelectedIndex();
        if (getSelectedTab() instanceof TaskTab tab) {
            currentEntry = tab.entry();
        } else {
            currentEntry = null;
        }
        taskEntryTreeGrid.controller().taskNetworkGraph().taskEntryDataProvider().rootEntry(currentEntry);
        taskEntryTreeGrid.settings().currentRootEntry(currentEntry);
        while (getComponentCount() > tabIndex + 1) {
            remove(getTabAt(tabIndex + 1));
        }
    }

    public void onSelectNewRootEntry(TaskEntry entry) {
        log.debug("onSelectNewRootEntry: " + entry);
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