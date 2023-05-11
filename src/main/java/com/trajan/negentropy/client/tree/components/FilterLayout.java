package com.trajan.negentropy.client.tree.components;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.stream.Collectors;

@Accessors(fluent = true)
@Getter
public class FilterLayout extends FormLayout {
    private final TreeViewPresenter presenter;

    private TextField name;
    private TagComboBox tagsToExclude;
    private TagComboBox tagsToInclude;
    private Button resetBtn;

    public FilterLayout(TreeViewPresenter presenter) {
        this.presenter = presenter;

        this.addClassName("filter-layout");

        configureFields();
        configureInteractions();
        configureLayout();
    }

    private void configureFields() {
        name = new TextField("Search By Name");
        name.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        name.setValueChangeMode(ValueChangeMode.EAGER);
        name.addValueChangeListener(event -> {
            TaskFilter filter = presenter.dataProvider().getActiveFilter();
            filter.name(name.getValue());
        });

        tagsToExclude = new TagComboBox("Filter: Excluded Tags", presenter);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToExclude.addValueChangeListener(event -> {
            TaskFilter filter = presenter.dataProvider().getActiveFilter();
            filter.excludedTagIds(tagsToExclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
        });

        tagsToInclude = new TagComboBox("Filter: Include Tags", presenter);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);
        tagsToInclude.addValueChangeListener(event -> {
            TaskFilter filter = presenter.dataProvider().getActiveFilter();
            filter.includedTagIds(tagsToInclude.getValue().stream()
                    .map(Tag::id)
                    .collect(Collectors.toSet()));
        });

        resetBtn = new Button("Reset");

        this.add(name, tagsToExclude, tagsToInclude, resetBtn);
    }

    private void configureInteractions() {
        resetBtn.addClickListener(e -> {
            name.clear();
            tagsToExclude.clear();
            tagsToInclude.clear();
            // TODO: Actually apply filters
        });
    }

    private void configureLayout() {
        this.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxSizing.BORDER);

        this.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("600px", 2),
                new ResponsiveStep("1200px", 4));

        this.setWidthFull();
    }
}