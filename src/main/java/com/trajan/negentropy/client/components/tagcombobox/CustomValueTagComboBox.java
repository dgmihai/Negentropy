package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CustomValueTagComboBox extends TagComboBox {
    private static final Logger logger = LoggerFactory.getLogger(CustomValueTagComboBox.class);

    protected Consumer<Tag> onCustomValueSet = tag -> {};

    public CustomValueTagComboBox(ClientDataController presenter) {
        super(presenter);
        init();
    }

    public CustomValueTagComboBox(ClientDataController presenter, Consumer<Tag> onCustomValueSet) {
        super(presenter);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    public CustomValueTagComboBox(String labelText, ClientDataController presenter, Consumer<Tag> onCustomValueSet) {
        super(labelText, presenter);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    @Override
    protected void init() {
        super.init();

        setAllowCustomValue(true);

        addCustomValueSetListener(event -> {
            String name = event.getDetail();
            Tag newTag = controller.createTag(new Tag(null, name));
            items.add(newTag);
            Set<Tag> tags = new HashSet<>(this.getValue());
            this.setItems(items);
            tags.add(newTag);
            this.setValue(tags);
            onCustomValueSet.accept(newTag);
        });
    }
}
