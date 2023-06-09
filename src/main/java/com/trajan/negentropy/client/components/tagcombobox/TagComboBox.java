package com.trajan.negentropy.client.components.tagcombobox;

import com.trajan.negentropy.client.controller.ClientDataController;
import com.trajan.negentropy.server.facade.model.Tag;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public class TagComboBox extends MultiSelectComboBox<Tag> {
    protected final ClientDataController controller;
    @Setter
    protected Set<Tag> items = new HashSet<>();

    public TagComboBox(ClientDataController controller) {
        super();
        this.controller = controller;
        this.
        init();
    }

    public TagComboBox(String labelText, ClientDataController controller) {
        super();
        this.setPlaceholder(labelText);
        this.controller = controller;
        init();
    }

    protected void init() {
        fetchTags();
        setItemLabelGenerator(Tag::name);
    }

    private void fetchTags() {
        items = controller.queryService().fetchAllTags().collect(Collectors.toSet());
        setItems(items);
    }
}
