package com.trajan.negentropy.view;

import com.trajan.negentropy.controller.ViewController;
import com.trajan.negentropy.data.entity.Task;
import com.trajan.negentropy.data.entity.Task_;
import com.trajan.negentropy.data.repository.Filter;
import com.trajan.negentropy.data.repository.QueryOperator;
import com.trajan.negentropy.view.util.DurationConverter;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TaskForm extends FormLayout {
    private static final Logger logger = LoggerFactory.getLogger(TaskForm.class);
    TextField name = new TextField("Name");
    TextArea description = new TextArea("Description");
    // X priority = new X("Priority");
    TextField duration = new TextField("Estimated Duration");
    ComboBox<Task> instanceParent = new ComboBox<>("Parent");

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    Binder<Task> binder = new BeanValidationBinder<>(Task.class);

    @Autowired
    private ViewController controller;

    @Autowired
    public TaskForm(ViewController controller) {
        this.controller = controller;
        addClassName("Entry-form");

        duration.setHelperText("Format: 1h 2m 30s");
        duration.setPattern(DurationConverter.DURATION_PATTERN);
        binder.forField(duration)
                .withConverter(new DurationConverter())
                .bind(Task::getDuration, Task::setDuration);

//        binder.forField(instanceParent).bind(
//                task -> {
//                    if(!task.getParents().isEmpty()) {
//                        return task.getParents().get(0);
//                    } else {
//                        return null;
//                    }
//                },
//                (task, parent) -> {
//                    task.newParent(this.instanceParent.getValue());
//                });
        binder.bindInstanceFields(this);
        instanceParent.setItemLabelGenerator(Task::getName);


        add(    name,
                duration,
                description,
                instanceParent,
                createButtonsLayout());
    }

    public void setTask(Task task) {
        this.save.setText("Update");
        this.delete.setEnabled(true);
        if (task.getChildren().size() > 0) {
            this.duration.setVisible(false);
        } else {
            this.duration.setVisible(true);
        }
        instanceParent.setItems(controller.findTasks(Filter.builder()
                .field(Task_.PK)
                .operator(QueryOperator.NOT_EQ)
                .value(task.getPk())
                .build()));
        binder.setBean(task);
    }

    public void clear() {
        this.save.setText("Add");
        this.delete.setEnabled(false);
        binder.setBean(new Task());
        logger.debug("Fetching tasks in TaskForm.clear");
        instanceParent.setItems(controller.findTasks());
    }

    // Events

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new ClearEvent(this)));

        name.setValueChangeMode(ValueChangeMode.EAGER);
        binder.addValueChangeListener(e -> save.setEnabled(binder.isValid()));
        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));
        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        if(binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }

    public static abstract class TaskFormEvent extends ComponentEvent<TaskForm> {
        private Task task;

        protected TaskFormEvent(TaskForm source, Task task) {
            super(source, false);
            this.task = task;
        }

        public Task getTask() {
            return task;
        }
    }

    public static class SaveEvent extends TaskFormEvent {
        SaveEvent(TaskForm source, Task task) {
            super(source, task);
        }
    }
    public static class DeleteEvent extends TaskFormEvent {
        DeleteEvent(TaskForm source, Task task) {
            super(source, task);
        }

    }
    public static class ClearEvent extends TaskFormEvent {
        ClearEvent(TaskForm source) {
            super(source, null);
        }
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }
    public Registration addCloseListener(ComponentEventListener<ClearEvent> listener) {
        return addListener(ClearEvent.class, listener);
    }
}