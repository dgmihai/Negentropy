package com.trajan.negentropy.client.components.fields;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants.EditorType;
import com.wontlost.ckeditor.Constants.ThemeType;
import com.wontlost.ckeditor.Constants.Toolbar;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

public class DescriptionTextArea {
    public static final Toolbar[] TOOLBAR = new Toolbar[] {
            Toolbar.bold, Toolbar.italic, Toolbar.strikethrough, Toolbar.link, Toolbar.bulletedList,
            Toolbar.numberedList, Toolbar.pipe, Toolbar.outdent, Toolbar.indent, Toolbar.pipe, Toolbar.undo,
            Toolbar.redo };

    public static TextArea textArea() {
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPlaceholder("Description");
        descriptionArea.setValueChangeMode(ValueChangeMode.EAGER);
        descriptionArea.setClearButtonVisible(true);
        return descriptionArea;
    }

    public static VaadinCKEditor inline(String placeholder) {
        Config config = new Config();
        config.setEditorToolBar(DescriptionTextArea.TOOLBAR);
        config.setTitle("");
        if (placeholder != null) {
            config.setPlaceHolder(placeholder);
        }

        VaadinCKEditor editor = new VaadinCKEditorBuilder().with(builder->{
            builder.editorType = EditorType.INLINE;
            builder.theme = ThemeType.DARK;
            builder.config = config;
            builder.margin = "-20px 0px 0px 0px";
        }).createVaadinCKEditor();

        editor.setLabel("");
        return editor;
    }
}
