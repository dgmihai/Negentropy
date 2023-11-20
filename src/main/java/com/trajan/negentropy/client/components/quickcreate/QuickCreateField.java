package com.trajan.negentropy.client.components.quickcreate;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider.HasTaskNodeProvider;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.TaskID;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import org.springframework.data.util.Pair;

public class QuickCreateField extends TextField implements HasTaskNodeProvider {
    @Getter
    private final UIController controller;

    @Getter
    private final TaskNodeProvider taskNodeProvider;

    private TaskDTO task = null;
    private TaskNodeInfoData<TaskNodeDTO> nodeDTO = null;

    public QuickCreateField(UIController controller) {
        super();
        this.controller = controller;
        this.taskNodeProvider = new TaskNodeProvider(controller) {
            @Override
            public TaskDTO getTask() {
                return task;
            }

            @Override
            public TaskNodeInfoData<?> getNodeInfo() {
                return nodeDTO;
            }

            @Override
            public boolean isValid() {
                String input = getValue();
                if (!input.isBlank()) {
                    try {
                        parse(input);
                        return true;
                    } catch (QuickCreateParser.ParseException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        };

        this.setClearButtonVisible(true);
        this.setValueChangeMode(ValueChangeMode.EAGER);
        this.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        String placeholder = K.QUICK_CREATE + "(name " +
                QuickCreateParser.DELIMITER + "desc description " +
                QuickCreateParser.DELIMITER + "tag tag1, tag2,... " +
                QuickCreateParser.DELIMITER + "dur 1h30m, " +
                QuickCreateParser.DELIMITER + "rec(curring) " +
                QuickCreateParser.DELIMITER + "top)";
        this.setPlaceholder(placeholder);

        Shortcuts.addShortcutListener(this,
                this::save,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                this::clear,
                Key.ESCAPE);

        this.addValueChangeListener(e -> {
            task = null;
            nodeDTO = null;
        });

        taskNodeProvider.afterSuccessfulSave(this::clear);
    }

    public void save() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);

                TaskNode rootNode = controller.activeTaskNodeDisplay().rootNode().orElse(null);
                TaskID rootTaskId = rootNode == null ? null : rootNode.task().id();
                taskNodeProvider.createNode(rootTaskId,
                        InsertLocation.LAST);
            } catch (QuickCreateParser.ParseException e) {
                NotificationMessage.error(e);
                this.setErrorMessage(e.getMessage());
            }
        }
    }

    public boolean isValid() {
        return taskNodeProvider.isValid();
    }

    private void parse(String input) throws QuickCreateParser.ParseException {
        Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
        task = result.getFirst();
        nodeDTO = result.getSecond();
    }

    public Task getTask() {
        return task;
    }

    public TaskNodeInfoData<TaskNodeDTO> getNodeInfo() {
        return nodeDTO;
    }
}
