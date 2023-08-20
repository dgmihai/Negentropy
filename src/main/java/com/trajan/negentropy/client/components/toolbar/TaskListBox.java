package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.components.filterform.FilterForm;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TaskListBox extends MultiSelectListBox<Task> implements TaskNodeProvider {
    @Getter
    protected final ClientDataController controller;
    protected final FilterForm form;

    public TaskListBox(ClientDataController controller, FilterForm form) {
        super();
        this.controller = controller;
        this.form = form;
        setItemLabelGenerator(Task::name);
    }

    public void fetchTasks(TaskFilter filter) {
        log.debug("FETCH TASKS: " + filter);
        if (!filter.isEmpty()) {
            setItems(controller.services().query().fetchTasks(filter).collect(Collectors.toSet()));
        } else {
            setItems();
        }
    }

    public void hideOtherTasks(Task task) {
        setItems(task);
        setValue(Set.of(task));
    }

    @Override
    public boolean isValid() {
        return this.getValue() != null;
    }

    @Override
    public Task getTask() {
        return this.getValue().stream().findFirst().orElse(null);
    }

    @Override
    public TaskNodeDTOData<?> getNodeInfo() {
        return new TaskNodeDTO();
    }
}
