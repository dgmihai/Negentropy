package com.trajan.negentropy.client.components.taskform;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;

import java.util.ArrayList;
import java.util.List;

public class ReadOnlySettableFormLayout extends FormLayout {
    public void setReadOnly(boolean readOnly) {
        this.getAllChildren(this).forEach(component -> {
            if (component instanceof HasValue<?,?> valueHolder) {
                valueHolder.setReadOnly(readOnly);
            }
        });
    }

    private List<Component> getAllChildren(Component component) {
        List<Component> components = new ArrayList<>();
        List<Component> children = component.getChildren().toList();

        for (Component child : children) {
            components.add(child);
            components.addAll(getAllChildren(child));
        }

        return components;
    }

    public void clearAllFields() {
        this.getChildren().forEach(component -> {
            if (component instanceof HasValue<?,?> valueHolder) {
                valueHolder.clear();
            }
        });
    }
}
