package com.trajan.negentropy.client.components.taskform;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;

public class ReadOnlySettableFormLayout extends FormLayout {

    public void setReadOnly(boolean readOnly) {
        this.getChildren().forEach(component -> {
            if (component instanceof HasValue<?,?> valueHolder) {
                valueHolder.setReadOnly(readOnly);
            }
        });
    }

    public void clearAllFields() {
        this.getChildren().forEach(component -> {
            if (component instanceof HasValue<?,?> valueHolder) {
                valueHolder.clear();
            }
        });
    }
}
