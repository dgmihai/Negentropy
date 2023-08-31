package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.model.Tag;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
public class TagComboBox extends MultiSelectComboBox<Tag> {
    protected final ClientDataController controller;
    @Setter
    @Getter
    protected Set<Tag> items = new HashSet<>();

    public TagComboBox(ClientDataController controller) {
        super();
        this.controller = controller;
        this.init();
    }

    public TagComboBox(String labelText, ClientDataController controller) {
        super();
        this.setPlaceholder(labelText);
        this.controller = controller;
        this.init();
    }

    protected void init() {
        this.fetchTags();
        this.setItemLabelGenerator(Tag::name);
    }

    private void fetchTags() {
        items = new HashSet<>(controller.taskNetworkGraph().tagMap().values());
        this.setItems(items);
    }
}
