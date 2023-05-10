package com.trajan.negentropy.client.components.taskform;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.tree.data.TaskEntry;
import com.trajan.negentropy.client.util.CustomValueTagComboBox;
import com.trajan.negentropy.client.util.DurationConverter;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public class TaskEntryFormLayout extends AbstractTaskFormLayout {
    @Getter
    private final Binder<TaskEntry> binder = new BeanValidationBinder<>(TaskEntry.class);

    //    IntegerField priorityField;

    public TaskEntryFormLayout(TreeViewPresenter presenter, TaskEntry entry) {
        super(presenter);
        binder.setBean(entry);

        configureFields();
        configureInteractions();
        configureBindings();
        configureLayout();

        this.add(nameField, durationField,
                tagComboBox, descriptionArea,
                buttonLayout);
    }

    @Override
    protected void configureFields() {
        super.configureFields();

//        priorityField = new IntegerField("Priority");
//        priorityField.setHelperText("0 -> 9");
//        priorityField.setMaxWidth("70px");
//        priorityField.setMax(9);
//        binder.forField(priorityField)
//                .bind("importance");
    }

    @Override
    boolean isValid() {
        return binder.isValid();
    }

    @Override
    protected void configureBindings() {
        binder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(
                        entry -> entry.task().name(),
                        (entry, name) -> entry.task().name(name));

        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(
                        entry -> entry.task().duration(),
                        (entry, duration) -> entry.task().duration(duration));

        binder.forField(descriptionArea)
                .bind(
                        entry -> entry.task().description(),
                        (entry, description) -> entry.task().description(description));

        tagComboBox = new CustomValueTagComboBox("Tags", presenter, tag ->{
            binder.getBean().task().tags().add(tag);
        });

        binder.forField(tagComboBox)
                .bind(
                        entry -> entry.task().tags(),
                        (entry, tags) -> entry.task().tags(tags));

        saveButton.setEnabled(binder.isValid());
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });
    }
}
