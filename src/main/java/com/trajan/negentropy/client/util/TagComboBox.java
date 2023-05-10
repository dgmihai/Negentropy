package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.server.facade.model.Tag;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
public class TagComboBox extends MultiSelectComboBox<Tag> {
    protected final TreeViewPresenter presenter;
    @Setter
    protected Set<Tag> items = new HashSet<>();

    public TagComboBox(TreeViewPresenter presenter) {
        super();
        this.presenter = presenter;
        init();
    }

    public TagComboBox(String labelText, TreeViewPresenter presenter) {
        super(labelText);
        this.presenter = presenter;
        init();
    }

    protected void init() {
        fetchTags();
        setItemLabelGenerator(Tag::name);
    }

    private void fetchTags() {
        items = presenter.queryService().fetchAllTags().collect(Collectors.toSet());
        setItems(items);
    }
}
