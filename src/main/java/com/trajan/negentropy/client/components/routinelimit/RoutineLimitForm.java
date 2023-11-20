package com.trajan.negentropy.client.components.routinelimit;

import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.routinelimit.RoutineSelect.SelectOptions;
import com.trajan.negentropy.client.components.tagcombobox.TagComboBox;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBoxVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.timepicker.TimePicker;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

import static com.trajan.negentropy.model.filter.TaskTreeFilter.INNER_JOIN_INCLUDED_TAGS;

@Component
@Scope("prototype")
@Getter
public class RoutineLimitForm extends FormLayout {
    @Autowired private UIController controller;

    private DurationTextField durationField = new DurationTextField();
    private IntegerField countField = new IntegerField();
    private TimePicker timePicker = new TimePicker();
    private RoutineSelect routineSelect = SpringContext.getBean(RoutineSelect.class);
    private TagComboBox tagsToExclude;
    private TagComboBox tagsToInclude;
    private Checkbox innerJoinTags = new Checkbox();

    @PostConstruct
    public void init() {
        durationField.setSizeFull();

        countField.setPlaceholder("Max Step Count");
        countField.setSizeFull();

        timePicker.setPlaceholder("Time Limit");
        timePicker.setSizeFull();

        tagsToInclude = new TagComboBox("Filter: Include Tags", controller);
        tagsToInclude.setClearButtonVisible(true);
        tagsToInclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);

        tagsToExclude = new TagComboBox("Filter: Exclude Tags", controller);
        tagsToExclude.setClearButtonVisible(true);
        tagsToExclude.addThemeVariants(MultiSelectComboBoxVariant.LUMO_SMALL);

        innerJoinTags.setLabel("Inner Join Tags");

        Hr hr = new Hr();

        hr.setVisible(false);
        tagsToInclude.setVisible(false);
        tagsToExclude.setVisible(false);
        innerJoinTags.setVisible(false);

        routineSelect.addValueChangeListener(e -> {
            if (routineSelect.getValue().equals(SelectOptions.WITH_CUSTOM_FILTER)) {
                tagsToInclude.setVisible(true);
                tagsToExclude.setVisible(true);
                innerJoinTags.setVisible(true);
                hr.setVisible(true);
            } else {
                tagsToInclude.setVisible(false);
                tagsToExclude.setVisible(false);
                innerJoinTags.setVisible(false);
                hr.setVisible(false);
            }
        });

        routineSelect.customFilterSupplier(() -> (RoutineLimitFilter) new RoutineLimitFilter()
                .includedTagIds(tagsToInclude.getValue()
                        .stream().map(Tag::id).collect(Collectors.toSet()))
                .excludedTagIds(tagsToExclude.getValue()
                        .stream().map(Tag::id).collect(Collectors.toSet()))
                .options(Set.of(INNER_JOIN_INCLUDED_TAGS))
                .completed(false));

        this.add(routineSelect, durationField, countField, timePicker, hr, tagsToInclude, tagsToExclude, innerJoinTags);
        this.setColspan(routineSelect, 2);
        this.setColspan(durationField, 2);
        this.setColspan(tagsToInclude, 2);
        this.setColspan(tagsToExclude, 2);
    }

    public RoutineLimitFilter getFilter() {
        Duration duration = durationField.isEmpty()
                ? null
                : DurationConverter.toModel(durationField.getValue()).getOrThrow(
                string -> {
                    String errorMessage = "Invalid duration: " + string;
                    NotificationMessage.error(errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });

        Integer count = countField.isEmpty()
                ? null
                : countField.getValue();

        LocalDateTime eta = null;
        if (!timePicker.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            LocalTime givenTime = timePicker.getValue();

            eta = now.withHour(givenTime.getHour())
                    .withMinute(givenTime.getMinute());

            if (eta.isBefore(now)) {
                eta = eta.plusDays(1);
            }
        }

        return routineSelect.getFilter()
                .durationLimit(duration)
                .stepCountLimit(count)
                .etaLimit(eta);
    }
}
