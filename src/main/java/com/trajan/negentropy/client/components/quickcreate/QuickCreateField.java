package com.trajan.negentropy.client.components.quickcreate;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.data.util.Pair;

@Accessors(fluent = true)
public class QuickCreateField extends TextField implements TaskNodeProvider {
    @Getter
    private ClientDataController controller;

    private Task task = null;
    private TaskNodeInfoData<TaskNodeDTO> nodeDTO = null;

    public QuickCreateField(ClientDataController controller) {
        super();
        this.controller = controller;

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

        afterSave(this::clear);
    }

    public void save() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);

                TaskNode result = save(controller.activeTaskNodeView().rootNodeId().orElse(null),
                        InsertLocation.LAST);
                if (result != null) {
                    this.clear();
                }
            } catch (QuickCreateParser.ParseException e) {
                NotificationError.show(e);
                this.setErrorMessage(e.getMessage());
            }
        }
    }

    @Override
    public boolean isValid() {
        String input = this.getValue();
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

    private void parse(String input) throws QuickCreateParser.ParseException {
        Pair<Task, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
        task = result.getFirst();
        nodeDTO = result.getSecond();
    }

    @Override
    public Task getTask() {
        return task;
    }

    @Override
    public TaskNodeInfoData<TaskNodeDTO> getNodeInfo() {
        return nodeDTO;
    }
}
