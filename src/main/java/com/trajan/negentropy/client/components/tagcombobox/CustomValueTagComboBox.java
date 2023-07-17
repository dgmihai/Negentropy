package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CustomValueTagComboBox extends TagComboBox {

    protected Consumer<Tag> onCustomValueSet = tag -> {};

    public CustomValueTagComboBox(ClientDataController controller) {
        super(controller);
        init();
    }

    public CustomValueTagComboBox(ClientDataController controller, Consumer<Tag> onCustomValueSet) {
        super(controller);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    public CustomValueTagComboBox(String labelText, ClientDataController controller, Consumer<Tag> onCustomValueSet) {
        super(labelText, controller);
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
