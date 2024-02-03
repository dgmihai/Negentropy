package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.controller.UIController;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Page;

public class TaskNodeInfoFormDialog extends TaskNodeInfoFormFullLayout {
    private final Dialog dialog;

    private void setMargins(int width) {
        if (width <= K.BREAKPOINT_PX) {
            dialog.addClassName("dialog-margins");
        } else {
            dialog.removeClassName("dialog-margins");
        }
    }

    public TaskNodeInfoFormDialog(Dialog dialog, UIController controller) {
        super(controller);
        this.dialog = dialog;

        Page page = UI.getCurrent().getPage();

        page.retrieveExtendedClientDetails(e ->
                this.setMargins(e.getWindowInnerWidth()));

        page.addBrowserWindowResizeListener(e ->
                this.setMargins(e.getWidth()));

        cancelButton.setText("Cancel");
        cancelButton.addClickListener(e -> dialog.close());

        onClose = dialog::close;

        onSaveSelect.setVisible(false);
        projectComboBox.setRequired(true);
        projectComboBox.getListDataView().getItems()
                .filter(project -> project.name().equals(controller.settings().DEFAULT_PROJECT()))
                .findFirst()
                .ifPresent(project -> projectComboBox.setValue(project));

        nodeInfoBinder.withValidator(node -> !projectComboBox.isEmpty(), "Valid parent is required");

        preexistingTaskSearchButton.setIcon(VaadinIcon.EXCLAMATION.create());
        preexistingTaskSearchButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    }
}
