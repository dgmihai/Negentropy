package com.trajan.negentropy.client.components.quickcreate;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TaskProvider;
import com.trajan.negentropy.client.util.TaskProviderException;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.util.Optional;

@Accessors(fluent = true)
public class QuickCreateField extends TextField implements TaskProvider {
    private static final Logger logger = LoggerFactory.getLogger(QuickCreateField.class);

    private ClientDataController presenter;

    private Task task = null;
    private TaskNodeInfo node = null;

    public QuickCreateField(ClientDataController presenter) {
        super();
        this.presenter = presenter;

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
            node = null;
        });
    }

    public void save() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);

                Response response = presenter.addTaskFromProvider(this, node);
                if (response.success()) {
                    this.clear();
                } else {
                    this.setErrorMessage(response.message());
                }
            } catch (QuickCreateParser.ParseException e) {
                NotificationError.show(e);
            }
        }
    }

    @Override
    public Response hasValidTask() {
        String input = this.getValue();
        if (!input.isBlank()) {
            try {
                parse(input);
                return new Response(true, K.OK);
            } catch (QuickCreateParser.ParseException e) {
                return new Response(false, e.getMessage());
            }
        } else {
            return new Response(false, K.QUICK_CREATE + " input is blank");
        }
    }

    private void parse(String input) throws QuickCreateParser.ParseException {
        Pair<Task, TaskNodeInfo> result = QuickCreateParser.parse(input);
        task = result.getFirst();
        node = result.getSecond();
    }

    @Override
    public Optional<Task> getTask() throws TaskProviderException {
        if (task != null) {
            return Optional.of(task);
        } else {
            return Optional.empty();
        }
    }
}
