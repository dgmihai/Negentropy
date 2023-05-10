package com.trajan.negentropy.client.tree.components;

import com.trajan.negentropy.client.tree.TreeViewPresenter;
import com.trajan.negentropy.client.util.TagComboBox;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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
        name = new TextField("Name");
        name.setPlaceholder("Task name");

        tagsToExclude = new TagComboBox("Exclude Tags", presenter);
        tagsToInclude = new TagComboBox("Include Tags", presenter);

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

    public TaskFilter getTaskFilter() {
        List<Predicate> predicates = new ArrayList<>();

        TaskFilter filter = new TaskFilter();

        if (!name.isEmpty()) {
            filter.name(name.getValue());
        }

        if (!tagsToInclude.isEmpty()) {
//            filter.includedTags(tagsToInclude.getValue());
        }

        if (!tagsToExclude.isEmpty()) {
//            filter.excludedTags(tagsToExclude.getValue());
        }

        return filter;
//            String lowerCaseFilter = name.getValue().toLowerCase();
//            Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get(TaskEntity_.NAME)),
//                    lowerCaseFilter + "%");
//            predicates.add(criteriaBuilder.and(nameMatch));
//        }

//        if (!name.isEmpty()) {
//            String lowerCaseFilter = name.getValue().toLowerCase();
//            Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get(TaskEntity_.NAME)),
//                    lowerCaseFilter + "%");
//            predicates.add(criteriaBuilder.and(nameMatch));
//        }
//
//        if (!tagsToExclude.isEmpty()) {
//            String databaseColumn = TaskEntity_.TAGS;
//            List<Predicate> tagExclusionPredicates = new ArrayList<>();
//            for (Tag tag : tagsToExclude.getValue()) {
//                tagExclusionPredicates
//                        .add(criteriaBuilder.notEqual(criteriaBuilder.literal(tag), root.get(databaseColumn)));
//            }
//            predicates.add(criteriaBuilder.and(tagExclusionPredicates.toArray(Predicate[]::new)));
//        }
//
//        if (!tagsToInclude.isEmpty()) {
//            String databaseColumn = TaskEntity_.TAGS;
//            List<Predicate> tagInclusionPredicates = new ArrayList<>();
//            for (Tag tag : tagsToExclude.getValue()) {
//                tagInclusionPredicates
//                        .add(criteriaBuilder.equal(criteriaBuilder.literal(tag), root.get(databaseColumn)));
//            }
//            predicates.add(criteriaBuilder.and(tagInclusionPredicates.toArray(Predicate[]::new)));
//        }
//
//        return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    }
}