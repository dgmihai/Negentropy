package com.trajan.negentropy.client.components.grid.subcomponents;

import com.trajan.negentropy.client.components.grid.TaskEntryTreeGrid;
import com.trajan.negentropy.client.controller.util.TaskEntry;
import com.trajan.negentropy.client.logger.UILogger;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;

import java.util.List;
import java.util.Stack;

public class NestedTaskTabs extends Tabs {
    private final UILogger log = new UILogger();

    private final TaskEntryTreeGrid taskEntryTreeGrid;
    private TaskEntry currentEntry;

    public NestedTaskTabs(TaskEntryTreeGrid taskEntryTreeGrid) {
        super();
        this.taskEntryTreeGrid = taskEntryTreeGrid;

        this.add(new Tab(new Icon(VaadinIcon.HOME)));

        this.selectNewRootEntry(taskEntryTreeGrid.settings().currentRootEntry());
        this.addSelectedChangeListener(e -> {
            if (e.isFromClient()) onSelectTab();
        });

        this.addThemeVariants(TabsVariant.LUMO_SMALL);
    }

    private synchronized void onSelectTab() {
        this.currentEntry = (getSelectedTab() instanceof TaskTab tab)
            ? tab.entry() : null;

        int selectedIndex = getSelectedIndex();
        while (getComponentCount() > selectedIndex + 1) {
            remove(getTabAt(selectedIndex + 1));
        }
        updateGrid();
    }

    private void updateGrid() {
        taskEntryTreeGrid.taskEntryDataProvider().rootEntry(currentEntry);
        taskEntryTreeGrid.settings().currentRootEntry(currentEntry);
    }

    public synchronized void selectNewRootEntry(TaskEntry entry) {
        List<TaskTab> tabs = this.getChildren()
                .filter(tab -> tab instanceof TaskTab)
                .map(tab -> (TaskTab) tab)
                .toList();
        if (entry == null) {
            log.debug("Selected null root entry.");
            this.setSelectedIndex(0);
        } else if (tabs.stream().
                anyMatch(tab -> tab.entry().equals(entry))) {
            log.debug("Selected root entry already in tab list.");
            this.setSelectedTab(tabs.stream()
                    .filter(tab -> tab.entry().equals(entry))
                    .findFirst()
                    .get());
        } else {
            log.debug("Selected root entry not in tab list.");
            this.getChildren()
                    .filter(tab -> tab instanceof TaskTab)
                    .map(tab -> (TaskTab) tab)
                    .forEach(this::remove);

            TaskEntry current = entry;
            Stack<TaskEntry> stack = new Stack<>();
            while (current != null) {
                log.debug("Adding tab for <" + current.task().name() + ">");
                stack.push(current);
                current = current.parent();
            }
            int count = 0;
            while (!stack.empty()) {
                count++;
                this.add(new TaskTab(stack.pop()));
            }

            this.setSelectedIndex(count);
        }
        this.onSelectTab();
    }
}