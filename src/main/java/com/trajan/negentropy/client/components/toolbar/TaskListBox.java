package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.components.searchandfilterform.AbstractSearchAndFilterForm;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TaskListBox extends MultiSelectListBox<Task> implements HasTaskNodeProvider {
    @Getter
    protected final UIController controller;
    protected final AbstractSearchAndFilterForm form;

    @Getter
    private final TaskNodeProvider taskNodeProvider;

    public TaskListBox(UIController controller, AbstractSearchAndFilterForm form) {
        super();
        this.controller = controller;

        this.taskNodeProvider = new TaskNodeProvider(controller) {
            @Override
            public boolean isValid() {
                return getValue() != null;
            }

            @Override
            public Task getTask() {
                return getValue().stream().findFirst().orElse(null);
            }

            @Override
            public TaskNodeDTOData<?> getNodeInfo() {
                return new TaskNodeDTO();
            }
        };

        this.form = form;
        setItemLabelGenerator(Task::name);
    }

    public void fetchTasks(TaskTreeFilter filter) {
        log.debug("FETCH TASKS: " + filter);
        if (!filter.isEmpty()) {
            setItems(controller.services().query().fetchAllTasks(filter).collect(Collectors.toSet()));
        } else {
            setItems();
        }
    }

    public void hideOtherTasks(Task task) {
        setItems(task);
        setValue(Set.of(task));
    }
}
