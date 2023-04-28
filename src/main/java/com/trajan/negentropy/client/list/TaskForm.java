//package com.trajan.negentropy.client.list;
//
//import com.trajan.negentropy.client.list.util.CreateTagDialog;
//import com.trajan.negentropy.client.util.DurationConverter;
//import com.trajan.negentropy.server.entity.TagEntity;
//import com.trajan.negentropy.server.entity.Task;
//import com.vaadin.flow.component.Key;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.button.ButtonVariant;
//import com.vaadin.flow.component.combobox.MultiSelectComboBox;
//import com.vaadin.flow.component.formlayout.FormLayout;
//import com.vaadin.flow.component.orderedlayout.FlexComponent;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.textfield.TextArea;
//import com.vaadin.flow.component.textfield.TextField;
//import com.vaadin.flow.data.binder.BeanValidationBinder;
//import com.vaadin.flow.data.binder.Binder;
//import com.vaadin.flow.data.value.ValueChangeMode;
//import lombok.Getter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class TaskForm extends FormLayout {
//    private static final Logger logger = LoggerFactory.getLogger(TaskForm.class);
//
//    ListViewPresenter presenter;
//    @Getter
//    private final Binder<Task> binder = new BeanValidationBinder<>(Task.class);
//
//    public Button save = new Button("Save");
//    public Button delete = new Button("Delete");
//    public Button close = new Button("Cancel");
//    public Button createTag = new Button("Create TagEntity");
//
//    public TextField titleField = new TextField("Title");
////    IntegerField priorityField = new IntegerField("Priority");
//    public TextField durationField = new TextField("Duration");
//    public TextArea descriptionArea = new TextArea("Notes");
//    public MultiSelectComboBox<TagEntity> tagBox = new MultiSelectComboBox<>("Tags");
//
//    public TaskForm(ListViewPresenter presenter) {
//        this.presenter = presenter;
//        addClassName("Entry-form");
//
//        configureFields();
//        configureButtons();
//        configureDragAndDrop();
//        configureLayout();
//
//        binder.setBean(new Task());
//        presenter.initTaskInfoForm(this);
//    }
//
//    private void configureFields() {
//        //titleField.setHelperText("Required");
//        titleField.setValueChangeMode(ValueChangeMode.EAGER);
//        binder.forField(titleField)
//                .asRequired("Title must exist and be unique")
//                .bind("title");
//
////        priorityField.setHelperText("0 -> 9");
////        priorityField.setMaxWidth("70px");
////        priorityField.setMax(9);
////        binder.forField(priorityField)
////                .bind("priority");
//
//        durationField.setHelperText("Format: 1h 2m 30s");
//        durationField.setPattern(DurationConverter.DURATION_PATTERN);
//        binder.forField(durationField)
//                .withConverter(new DurationConverter())
//                .bind("duration");
//
//        binder.forField(descriptionArea)
//                .bind("description");
//
//        tagBox.setItems(presenter.findAllTags());
//        tagBox.setItemLabelGenerator(TagEntity::getName);
//        tagBox.setWidthFull();
//        binder.forField(tagBox)
//                .bind(Task::getTags, Task::setTags);
//    }
//
//    private void configureButtons() {
//        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
//        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
//        createTag.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//
//        save.addClickShortcut(Key.ENTER);
//        close.addClickShortcut(Key.ESCAPE);
//
//        save.addClickListener(e -> {
//            presenter.saveTaskFromForm();
//            presenter.clearSelectedTask();
//        });
//        delete.addClickListener(e -> presenter.deleteTask(binder.getBean()));
//        close.addClickListener(e -> presenter.clearSelectedTask());
//        createTag.addClickListener( e -> {
//            CreateTagDialog dialog = new CreateTagDialog();
//            dialog.onSave(() -> {
//                TagEntity tag = dialog.getTag();
//                presenter.createTag(tag);
//            });
//            dialog.open();
//        });
//
//        binder.addValueChangeListener(e -> {
//            save.setEnabled(presenter.isValid());
//            delete.setEnabled(binder.getBean().getId() != null);
//        });
//    }
//
//    private void configureDragAndDrop() {
//        // TODO: TaskForm draggable or no?
////        DragSource<TaskForm> dragSource = DragSource.create(this);
////        dragSource.setDragData(this.binder.getBean());
////        binder.addValueChangeListener(e -> {
////            dragSource.setDraggable(presenter.isValid());
////        });
//    }
//
//    private void configureLayout() {
//        HorizontalLayout buttonLayout = new HorizontalLayout(
//                save,
//                close,
//                createTag);
//
//        this.setResponsiveSteps(
//                new ResponsiveStep("0", 1),
//                new ResponsiveStep("600px", 2));
////                new ResponsiveStep("900px", 4),
////                new ResponsiveStep("1200px", 5),
////                new ResponsiveStep("1500px", 6),
////                new ResponsiveStep("1800px", 7));
//
////        save.setWidth("32%");
////        close.setWidth("32%");
////        createTag.setWidth("32%");
//        buttonLayout.setWidthFull();
//        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
//        descriptionArea.setWidthFull();
//        setColspan(descriptionArea, 2);
//        setColspan(buttonLayout, 2);
//        setColspan(tagBox, 2);
//
//        this.setWidthFull();
//
//        this.add(
//                titleField,
//                durationField,
//                tagBox,
//                descriptionArea,
//                buttonLayout);
//    }
//}
