package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;

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
            Change persistTagChange = new PersistChange<>(
                    new Tag(null, name));
            DataMapResponse response =  controller.requestChange(persistTagChange);
            Tag newTag = (Tag) response.changeRelevantDataMap().getFirst(persistTagChange.id());
            items.add(newTag);
            this.setItems(items);
            Set<Tag> tags = new HashSet<>(this.getValue());
            tags.add(newTag);
            this.setValue(tags);
            onCustomValueSet.accept(newTag);
        });
    }
}
