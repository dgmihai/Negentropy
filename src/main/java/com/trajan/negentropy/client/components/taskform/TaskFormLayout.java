package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.components.tagcombobox.CustomValueTagComboBox;
import com.trajan.negentropy.client.util.DurationConverter;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class TaskFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<Task> binder = new BeanValidationBinder<>(Task.class);

    public TaskFormLayout(TreeViewPresenter presenter, Task task) {
        super(presenter);
        binder.setBean(task);

        configureFields();
        configureInteractions();
        configureBindings();
        configureLayout();

        this.add(nameField, durationField,
                tagComboBox, descriptionArea,
                buttonLayout);
    }

    @Override
    boolean isValid() {
        return binder.isValid();
    }

    @Override
    protected void configureBindings() {
        binder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(Task::name, Task::name);

        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(Task::duration, Task::duration);

        binder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox = new CustomValueTagComboBox(presenter, tag ->{
            binder.getBean().tags().add(tag);
        });
        tagComboBox.setPlaceholder("Tags");

        binder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }
}
