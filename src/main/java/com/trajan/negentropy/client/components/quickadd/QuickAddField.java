package com.trajan.negentropy.client.components.quickadd;

import com.trajan.negentropy.client.util.NotificationError;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.response.Response;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class QuickAddField extends TextField {
    Function<Task, Response> onAction;
    private static final Logger logger = LoggerFactory.getLogger(QuickAddField.class);

    public QuickAddField(Function<Task, Response> onAction) {
        super("Quick Add");

        this.onAction = onAction;
        this.setValueChangeMode(ValueChangeMode.EAGER);
        this.addThemeVariants(TextFieldVariant.LUMO_SMALL);

        this.setHelperText("Format: name " +
                QuickAddParser.DELIMITER + "desc description " +
                QuickAddParser.DELIMITER + "tag tag1, tag2,... " +
                QuickAddParser.DELIMITER + "dur seconds, " +
                QuickAddParser.DELIMITER + "rep(eating)");

        Shortcuts.addShortcutListener(this,
                this::apply,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                this::clear,
                Key.ESCAPE);
    }

    public void apply() {
        String input = this.getValue();
        logger.debug("QuickAdd input: " + input);
        if (!input.isBlank()) {
            try {
                Task task = QuickAddParser.parse(input);

                Response response = onAction.apply(task);
                if (response.success()) {
                    this.clear();
                } else {
                    this.setErrorMessage(response.message());
                }
            } catch (QuickAddParser.ParseException e) {
                NotificationError.show(e.getMessage());
            }
        }
    }
}
