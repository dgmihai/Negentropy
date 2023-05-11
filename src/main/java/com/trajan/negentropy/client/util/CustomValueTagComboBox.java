package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.server.facade.model.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CustomValueTagComboBox extends TagComboBox {

    protected Consumer<Tag> onCustomValueSet = tag -> {};

    public CustomValueTagComboBox(TreeViewPresenter presenter) {
        super(presenter);
        init();
    }

    public CustomValueTagComboBox(TreeViewPresenter presenter, Consumer<Tag> onCustomValueSet) {
        super(presenter);
        this.onCustomValueSet = onCustomValueSet;
        init();
    }

    public CustomValueTagComboBox(String labelText, TreeViewPresenter presenter, Consumer<Tag> onCustomValueSet) {
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
            Tag newTag = presenter.createTag(new Tag(null, name));
            items.add(newTag);
            Set<Tag> tags = new HashSet<>(this.getValue());
            this.setItems(items);
            tags.add(newTag);
            this.setValue(tags);
            onCustomValueSet.accept(newTag);
        });
    }
}
