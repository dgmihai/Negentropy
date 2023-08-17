package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Tag;

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
//            Change persistTagChange = Change.persist(
//                    new Tag(null, name));
//            DataMapResponse response = (DataMapResponse) controller.requestChange(persistTagChange);
//            Tag newTag = (Tag) response.changeRelevantDataMap().getFirst(persistTagChange.id());
//            items.add(newTag);
//            Set<Tag> tags = new HashSet<>(this.getValue());
//            this.setItems(items);
//            tags.add(newTag);
//            this.setValue(tags);
//            onCustomValueSet.accept(newTag);
        });
    }
}
