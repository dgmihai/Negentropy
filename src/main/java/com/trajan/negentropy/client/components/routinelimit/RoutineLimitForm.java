package com.trajan.negentropy.client.components.routinelimit;

import com.trajan.negentropy.client.components.fields.DurationTextField;
import com.trajan.negentropy.client.components.taskform.fields.EffortConverter;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.client.session.SessionServices;
import com.trajan.negentropy.client.session.UserSettings;
import com.trajan.negentropy.client.util.IndeterminateToggleButton;
import com.trajan.negentropy.client.util.NotificationMessage;
import com.trajan.negentropy.client.util.duration.DurationConverter;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.trajan.negentropy.model.filter.TaskTreeFilter.INNER_JOIN_INCLUDED_TAGS;

@Component
@Scope("prototype")
@Getter
public class RoutineLimitForm extends FormLayout {
    @Autowired private UIController controller;
    @Autowired private SessionServices services;
    @Autowired private UserSettings settings;

    private DurationTextField durationField = new DurationTextField();
    private IntegerField countField = new IntegerField();
    private Select<String> effortSelect = new Select<>();
    private TimePicker timePicker = new TimePicker();
    private Set<Tag> tagsToExclude = new HashSet<>();
    private Set<Tag> tagsToInclude = new HashSet<>();
    private Checkbox innerJoinTags = new Checkbox();
    private FormLayout toggleableTags = new FormLayout();

    @PostConstruct
    public void init() {
        durationField.setSizeFull();

        countField.setPlaceholder("Max Step Count");
        countField.setSizeFull();

        effortSelect.setPlaceholder("Effort");
        effortSelect.setSizeFull();
        effortSelect.setTooltipText("Effort");
        effortSelect.setItems(EffortConverter.DEFAULT_EFFORT_STRING, "0", "1", "2", "3", "4", "5");
        effortSelect.setValue(EffortConverter.toPresentation(services.routine().effortMaximum()));
        effortSelect.addValueChangeListener(e -> {
            services.routine().setEffortMaximum(EffortConverter.toModel(e.getValue()).getOrThrow(
                    string -> {
                        String errorMessage = "Invalid effort: " + string;
                        NotificationMessage.error(errorMessage);
                        return new IllegalArgumentException(errorMessage);
                    }));
        });

        timePicker.setPlaceholder("Time Limit");
        timePicker.setSizeFull();

        toggleableTags.setWidthFull();
        toggleableTags.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("10em", 2),
                new FormLayout.ResponsiveStep("20em", 3),
                new FormLayout.ResponsiveStep("30em", 4),
                new FormLayout.ResponsiveStep("40em", 5),
                new FormLayout.ResponsiveStep("50em", 6),
                new FormLayout.ResponsiveStep("60em", 7),
                new FormLayout.ResponsiveStep("70em", 8),
                new FormLayout.ResponsiveStep("80em", 9),
                new FormLayout.ResponsiveStep("90em", 10),
                new FormLayout.ResponsiveStep("100em", 11),
                new FormLayout.ResponsiveStep("110em", 12));
//        toggleableTags.setJustifyContentMode(JustifyContentMode.CENTER);
//        toggleableTags.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        innerJoinTags.setLabel("Inner Join Tags");

        this.add(durationField, countField, effortSelect, timePicker, toggleableTags);
        this.setColspan(durationField, 3);
        this.setColspan(toggleableTags, 3);
    }

    public void setToggleableTags(Set<Tag> tags) {
        toggleableTags.removeAll();
        tags.forEach(tag -> {
            IndeterminateToggleButton toggleButton = new IndeterminateToggleButton(
                    tag.name(),
                    state -> {
                        if (state == null) {
                            tagsToInclude.remove(tag);
                            tagsToExclude.remove(tag);
                        } else if (state) {
                            tagsToInclude.add(tag);
                            tagsToExclude.remove(tag);
                        } else {
                            tagsToInclude.remove(tag);
                            tagsToExclude.add(tag);
                        }
                    });
            toggleableTags.add(toggleButton);
            if(settings.filter().includedTagIds().contains(tag.id())) {
                toggleButton.setState(true);
            } else if(settings.filter().excludedTagIds().contains(tag.id())) {
                toggleButton.setState(false);
            } else {
                toggleButton.setState(null);
            }
        });
        toggleableTags.add(innerJoinTags);
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

        Integer effort = EffortConverter.toModel(effortSelect.getValue()).getOrThrow(
                string -> {
                    String errorMessage = "Invalid effort: " + string;
                    NotificationMessage.error(errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });
        if (effort == -1) effort = null;

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

        Set<String> options = (innerJoinTags.getValue())
                ? Set.of(INNER_JOIN_INCLUDED_TAGS)
                : Set.of();

        return (RoutineLimitFilter) new RoutineLimitFilter()
                .durationLimit(duration)
                .stepCountLimit(count)
                .etaLimit(eta)
                .effortMaximum(effort)
                .completed(false)
                .includedTagIds(tagsToInclude.stream()
                        .map(Tag::id).collect(Collectors.toSet()))
                .excludedTagIds(tagsToExclude.stream()
                        .map(Tag::id).collect(Collectors.toSet()))
                .options(options);
    }
}
