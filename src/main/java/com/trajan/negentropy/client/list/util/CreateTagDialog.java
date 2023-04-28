package com.trajan.negentropy.client.list.util;


import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;

public class CreateTagDialog extends Dialog {
    private TextField tagNameField;
    private Binder<TagEntity> binder;

    public CreateTagDialog() {
        initLayout();
        configureBinder();
    }

    private void initLayout() {
        VerticalLayout layout = new VerticalLayout();

        Label titleLabel = new Label("Create a new tag");
        tagNameField = new TextField("TagEntity name");

        layout.add(titleLabel, tagNameField);

        this.add(layout);
    }

    private void configureBinder() {
        binder = new Binder<>(TagEntity.class);
        binder.forField(tagNameField)
                .withValidator(new StringLengthValidator("TagEntity name cannot be empty", 1, null))
                .bind(TagEntity::name, TagEntity::name);
    }

    public void onSave(Runnable onSave) {
        tagNameField.addKeyDownListener(Key.ENTER, event -> {
            if (binder.validate().isOk()) {
                onSave.run();
                this.close();
            }
        });

        tagNameField.addKeyDownListener(Key.ESCAPE, event -> {
            this.close();
        });
    }

    public TagEntity getTag() {
        TagEntity tag = new TagEntity("");
        binder.writeBeanIfValid(tag);
        return tag;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        tagNameField.focus();
    }
}
