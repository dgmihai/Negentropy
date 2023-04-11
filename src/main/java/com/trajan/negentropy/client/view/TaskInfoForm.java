package com.trajan.negentropy.client.view;

import com.trajan.negentropy.client.controller.event.ViewEventPublisher;
import com.trajan.negentropy.client.util.DurationConverter;
import com.trajan.negentropy.server.entity.TaskInfo;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class TaskInfoForm extends FormLayout {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfoForm.class);
    private final ViewEventPublisher viewEventPublisher;

    @Getter
    private final Binder<TaskInfo> binder = new BeanValidationBinder<>(TaskInfo.class);

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    TextField titleField = new TextField("Title");
    IntegerField priorityField = new IntegerField("Priority");
    TextField durationField = new TextField("Duration");
    TextArea descriptionArea = new TextArea("Notes");

    public TaskInfoForm(ViewEventPublisher viewEventPublisher) {
        this.viewEventPublisher = viewEventPublisher;
        ;
        addClassName("Entry-form");

        configureFields();
        configureButtons();
        configureDragAndDrop();
        configureLayout();
        // Layout

        setTaskInfoBean(null);
    }

    private void configureFields() {
        //titleField.setHelperText("Required");
        titleField.setValueChangeMode(ValueChangeMode.EAGER);
        binder.forField(titleField)
                .asRequired("Title must exist and be unique")
                .bind("title");


        priorityField.setHelperText("0 -> 9");
        priorityField.setMaxWidth("70px");
        priorityField.setMax(9);
        binder.forField(priorityField)
                .bind("priority");


        durationField.setHelperText("Format: 1h 2m 30s");
        durationField.setPattern(DurationConverter.DURATION_PATTERN);
        binder.forField(durationField)
                .withConverter(new DurationConverter())
                .bind("duration");


        binder.forField(descriptionArea)
                .bind("description");
    }

    private void configureButtons() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> viewEventPublisher.
                publishTaskInfoFormEvent_Delete(this, binder.getBean()));
//        //close.addClickListener(event -> fireEvent(new ClearEvent(this)));

        binder.addValueChangeListener(e -> {
            save.setEnabled(binder.isValid());
            delete.setEnabled(binder.getBean().getId() != null);
        });
    }

    private void configureDragAndDrop() {
        DragSource<TaskInfoForm> dragSource = DragSource.create(this);
        dragSource.setDragData(this.getTaskInfoBean());
        binder.addValueChangeListener(e -> {
            dragSource.setDraggable(binder.isValid());
        });
    }

    private void configureLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout(
                save,
                delete,
                close);
        //buttonLayout.setAlignItems(FlexComponent.Alignment.STRETCH);



        setResponsiveSteps(
                new ResponsiveStep("0", 6),
                new ResponsiveStep("600px", 3));
//                new ResponsiveStep("900px", 4),
//                new ResponsiveStep("1200px", 5),
//                new ResponsiveStep("1500px", 6),
//                new ResponsiveStep("1800px", 7));

        save.setWidth("30%");
        delete.setWidth("30%");
        close.setWidth("30%");
        descriptionArea.setWidthFull();
        setColspan(descriptionArea, 3);
        setColspan(buttonLayout, 3);

        add(    titleField,
                priorityField,
                durationField,
                descriptionArea,
                buttonLayout
        );
    }

    public TaskInfo getTaskInfoBean() {
        return binder.getBean();
    }

    public void setTaskInfoBean(TaskInfo taskInfo) {
        binder.setBean(Objects.requireNonNullElseGet(taskInfo, TaskInfo::new));
    }

    private void validateAndSave() {
        if(binder.isValid()) {
            viewEventPublisher.publishTaskInfoFormEvent_Save(this, binder.getBean());
        }
    }
}
