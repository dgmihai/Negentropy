package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.TreeView;
import com.trajan.negentropy.client.components.toolbar.ToolbarTabSheet;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Page;

public class TaskNodeInfoFormDialog extends TaskNodeInfoFormFullLayout {
    private final Dialog dialog;
    private Button toTreeViewButton;

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
        dialog.setCloseOnOutsideClick(false);

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
    }

    @Override
    public void configureFields() {
        super.configureFields();
        toTreeViewButton = new Button();
        Icon toTreeViewIcon = VaadinIcon.TREE_TABLE.create();
        toTreeViewIcon.addClassName(K.ICON_COLOR_PRIMARY);
        toTreeViewButton.setIcon(toTreeViewIcon);
    }

    @Override
    public void configureInteractions() {
        super.configureInteractions();
        toTreeViewButton.addClickListener(e -> {
            Task pendingTask = this.taskNodeProvider.getTask();
            TaskNodeDTO nodeDTO = new TaskNodeDTO(this.taskNodeProvider.getNodeInfo());
            UI ui = UI.getCurrent();
            ui.navigate(TreeView.class);
            try {
                TreeView treeView = (TreeView) ui.getCurrentView();
                ToolbarTabSheet toolbarTabSheet = treeView.toolbarTabSheet();

                toolbarTabSheet.setCreateTaskFormData(pendingTask, nodeDTO);
                toolbarTabSheet.setSelectedTab(toolbarTabSheet.createNewTaskTab());
                cancelButton.click();
            } catch (Exception ex) {
                NotificationMessage.error(ex.getMessage());
            }
        });
    }

    @Override
    public void configureLayout() {
        super.configureLayout();
        toTreeViewButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
    }

    @Override
    public HorizontalLayout getInnerButtonLayout() {
        HorizontalLayout innerButtonLayout = super.getInnerButtonLayout();
        innerButtonLayout.removeAll();
        innerButtonLayout.setSpacing(false);
        saveButton.setWidth("63%");
        cancelButton.setWidth("23%");
        innerButtonLayout.add(saveButton, toTreeViewButton, cancelButton);
        return innerButtonLayout;
    }
}
