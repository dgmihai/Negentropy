package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.controller.UIController;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;

public class TaskNodeInfoFormDialog extends TaskNodeInfoFormFullLayout {
    private final Dialog dialog;

    public TaskNodeInfoFormDialog(Dialog dialog, UIController controller) {
        super(controller);
        this.dialog = dialog;

        clearButton.addClickListener(e -> dialog.close());

        onClose = dialog::close;

        onSaveSelect.setVisible(false);
        projectComboBox.setRequired(true);
        projectComboBox.getListDataView().getItems()
                .filter(project -> project.name().equals(controller.settings().DEFAULT_PROJECT()))
                .findFirst()
                .ifPresent(project -> projectComboBox.setValue(project));

        nodeInfoBinder.withValidator(node -> !projectComboBox.isEmpty(), "Valid project is required");

        preexistingTaskSearchButton.setIcon(VaadinIcon.EXCLAMATION.create());
        preexistingTaskSearchButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    }
}
