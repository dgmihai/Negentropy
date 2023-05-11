package com.trajan.negentropy.client.components.quickadd;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.client.util.TaskProvider;
import com.trajan.negentropy.client.util.TaskProviderException;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

@Accessors(fluent = true)
public class QuickCreateField extends TextField implements TaskProvider {
    private static final Logger logger = LoggerFactory.getLogger(QuickCreateField.class);

    private Task task = null;
    @Setter
    private Function<Task, Response> onAction;

    public QuickCreateField() {
        super("Quick Create");

        this.setValueChangeMode(ValueChangeMode.EAGER);
        this.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        this.setHelperText("Format: name " +
                QuickCreateParser.DELIMITER + "desc description " +
                QuickCreateParser.DELIMITER + "tag tag1, tag2,... " +
                QuickCreateParser.DELIMITER + "dur seconds, " +
                QuickCreateParser.DELIMITER + "rep(eating)" +
                QuickCreateParser.DELIMITER + "top");

        Shortcuts.addShortcutListener(this,
                this::save,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                this::clear,
                Key.ESCAPE);

        this.addValueChangeListener(e -> task = null);
    }

    public void save() {
        String input = this.getValue();
        logger.debug("Quick Create input: " + input);
        if (!input.isBlank()) {
            try {
                parse(input);

                Response response = onAction.apply(task);
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
            return new Response(false, "Quick Create input is blank");
        }
    }

    private void parse(String input) throws QuickCreateParser.ParseException {
        task = QuickCreateParser.parse(input);
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
