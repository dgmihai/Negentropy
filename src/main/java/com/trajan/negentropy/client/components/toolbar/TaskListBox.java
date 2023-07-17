package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.data.TaskProvider;
import com.trajan.negentropy.client.controller.data.TaskProviderException;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TaskListBox extends MultiSelectListBox<Task> implements TaskProvider {
    protected final ClientDataController controller;

    public TaskListBox(ClientDataController controller) {
        super();
        this.controller = controller;
        setItemLabelGenerator(Task::name);
    }

    public void fetchTasks(TaskFilter filter) {
        log.debug("FETCH TASKS: " + filter);
        if (!filter.isEmpty()) {
            log.debug("NONEMPTY FILETER");
            setItems(controller.queryService().fetchTasks(filter).collect(Collectors.toSet()));
        } else {
            log.debug("EMPTY FILETER");
            setItems();
        }
    }

    public void hideOtherTasks(Task task) {
        setItems(task);
        setValue(Set.of(task));
    }

    @Override
    public Response hasValidTask() {
        if (this.getValue() != null) {
            return Response.OK();
        } else {
            return new Response(false, "No valid existing task selected.");
        }
    }

    @Override
    public Optional<Task> getTask() throws TaskProviderException {
        return this.getValue().stream().findFirst();
    }

    @Override
    public TaskNodeInfo getNodeInfo() {
        return new TaskNodeInfo();
    }
}
