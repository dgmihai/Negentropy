package com.trajan.negentropy.client.tree;

import com.trajan.negentropy.client.tree.util.CreateTagDialog;
import com.trajan.negentropy.client.util.DurationConverter;
import com.trajan.negentropy.client.util.TagComboBox;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.facade.model.Task;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Accessors(fluent = true)
@Getter
public class TaskForm extends FormLayout {
    private static final Logger logger = LoggerFactory.getLogger(TaskForm.class);

    private final TreeViewPresenter presenter;

    private final Binder<Task> binder = new BeanValidationBinder<>(Task.class);

    private final Button saveButton = new Button("Save");
    private final Button clearButton = new Button("Cancel");
    public Button createTag = new Button("Create Tag");

    public TextField nameField = new TextField("Name");
//    IntegerField priorityField = new IntegerField("Priority");
    public TextField durationField = new TextField("Duration");
    public TextArea descriptionArea = new TextArea("Description");
    public TagComboBox tagComboBox;

    public TaskForm(TreeViewPresenter presenter) {
        this.presenter = presenter;

        tagComboBox = new TagComboBox("Tags", presenter.tagService());

        addClassName("Entry-form");

        configureFields();
        configureEvents();
        configureLayout();
    }

    private void configureFields() {
        nameField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(nameField)
                .asRequired("Name must exist and be unique")
                .bind(Task::name, Task::name);

//        priorityField.setHelperText("0 -> 9");
//        priorityField.setMaxWidth("70px");
//        priorityField.setMax(9);
//        binder.forField(priorityField)
//                .bind("priority");

        durationField.setHelperText("Format: 1h 2m 30s");
        durationField.setPattern(DurationConverter.DURATION_PATTERN);
        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind(Task::duration, Task::duration);

        binder.forField(descriptionArea)
                .bind(Task::description, Task::description);

        tagComboBox.setWidthFull();
        binder.forField(tagComboBox)
                .bind(Task::tags, Task::tags);
    }

    public void configureEvents() {
        Runnable clear = () -> {
            binder.setBean(new Task(null));
        };

        Runnable save = () -> {
            if (binder.isValid()) {
                presenter.onTaskFormSave();
                binder.setBean(new Task(null));
            }
        };

        saveButton.addClickListener(event -> save.run());
        clearButton.addClickListener(event -> clear.run());

        Shortcuts.addShortcutListener(this,
                save::run,
                Key.ENTER);

        Shortcuts.addShortcutListener(this,
                clear::run,
                Key.ESCAPE);

        saveButton.setEnabled(false);
        binder.addValueChangeListener(e -> {
            saveButton.setEnabled(binder.isValid());
        });

        clearButton.addClickListener(e -> {
            binder.setBean(new Task(null));
        });

        createTag.addClickListener( e -> {
            CreateTagDialog dialog = new CreateTagDialog();
            dialog.onSave(() -> {
                TagEntity tag = dialog.getTag();
                presenter.createTag(tag);
            });
            dialog.open();
        });
    }

    private void configureLayout() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(
                saveButton,
                clearButton,
                createTag);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2));
//                new ResponsiveStep("900px", 4),
//                new ResponsiveStep("1200px", 5),
//                new ResponsiveStep("1500px", 6),
//                new ResponsiveStep("1800px", 7));

//        save.setWidth("32%");
//        close.setWidth("32%");
//        createTag.setWidth("32%");
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        descriptionArea.setWidthFull();
        setColspan(descriptionArea, 2);
        setColspan(buttonLayout, 2);
        setColspan(tagComboBox, 2);

        this.setWidthFull();

        this.add(
                nameField,
                durationField,
                tagComboBox,
                descriptionArea,
                buttonLayout);
    }
}
