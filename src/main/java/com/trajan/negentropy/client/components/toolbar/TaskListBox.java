package com.trajan.negentropy.client.components.toolbar;

import com.trajan.negentropy.client.components.searchandfilterform.AbstractSearchAndFilterForm;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import lombok.Getter;

import java.util.Set;
import java.util.stream.Collectors;

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
            public TaskDTO getTask() {
                Task task = getValue().stream().findFirst().orElse(null);
                return (task != null)
                        ? new TaskDTO(task, controller.taskNetworkGraph().getTags(task.id()))
                        : null;
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
