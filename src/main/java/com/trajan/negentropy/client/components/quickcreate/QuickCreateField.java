package com.trajan.negentropy.client.components.quickcreate;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.client.controller.util.TaskNodeProvider;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.server.facade.response.Response;
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
    }

    public void save() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);

                saveRequest(null, InsertLocation.CHILD);
            } catch (QuickCreateParser.ParseException e) {
                NotificationError.show(e);
                onFailedSave(new Response(false, e.getMessage()));
            }
        }
    }

    @Override
    public Response hasValidTask() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);
                return Response.ok();
            } catch (QuickCreateParser.ParseException e) {
                return new Response(false, e.getMessage());
            }
        } else {
            return new Response(false, K.QUICK_CREATE + " input is blank");
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

    @Override
    public void onSuccessfulSave(HasTaskNodeData data) {
        this.clear();
    }

    @Override
    public void onFailedSave(Response response) {
        this.setErrorMessage(response.message());
    }
}
