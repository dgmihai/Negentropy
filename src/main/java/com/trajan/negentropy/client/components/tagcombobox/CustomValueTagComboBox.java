package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class CustomValueTagComboBox extends TagComboBox {

    protected BiConsumer<Set<Tag>, Set<Tag>> onCustomValueSet = (old, updated) -> {};

    public CustomValueTagComboBox(UIController controller) {
        super(controller);
        init();
    }

    public CustomValueTagComboBox(UIController controller, BiConsumer<Set<Tag>, Set<Tag>> onCustomValueSet) {
        super(controller);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    public CustomValueTagComboBox(String labelText, UIController controller, BiConsumer<Set<Tag>, Set<Tag>> onCustomValueSet) {
        super(labelText, controller);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    @Override
    protected void init() {
        super.init();

        setAllowCustomValue(true);

        addCustomValueSetListener(event -> {
            Set<Tag> oldValues = this.getValue();
            String name = event.getDetail().trim();
            Change persistTagChange = new PersistChange<>(
                    new Tag(null, name));
            DataMapResponse response = controller.requestChange(persistTagChange);
            Tag newTag = (Tag) response.changeRelevantDataMap().getFirst(persistTagChange.id());
            Set<Tag> tags = new HashSet<>(this.getValue());
            items.add(newTag);
            controller.taskNetworkGraph().tagMap().put(newTag.id(), newTag);
            instances.forEach(TagComboBox::fetchTags);
            tags.add(newTag);
            this.setValue(tags);
            Set<Tag> newValues = this.getValue();
            onCustomValueSet.accept(oldValues, newValues);
        });
    }
}
