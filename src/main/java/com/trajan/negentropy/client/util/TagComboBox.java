package com.trajan.negentropy.client.util;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.facade.TagService;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;

public class TagComboBox extends MultiSelectComboBox<TagEntity> {
    private final TagService tagService;

    public TagComboBox(TagService tagService) {
        super();
        this.tagService = tagService;
        init();
    }

    public TagComboBox(String label, TagService tagService) {
        super(label);
        this.tagService = tagService;
        init();
    }

    private void init() {
        setItemLabelGenerator(TagEntity::name);
        setItems(tagService.findAll());
    }
}
