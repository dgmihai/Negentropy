package com.trajan.negentropy.client.components.routinelimit;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.client.RoutineView;
import com.trajan.negentropy.client.controller.UIController;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.util.SpringContext;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class StartRoutineDialog {
    @Autowired private UIController controller;

    @Getter private final Dialog dialog = new Dialog();
    private List<PersistedDataDO> data;
    private RoutineLimitForm limitForm;

    @PostConstruct
    public void init() {
        dialog.setHeaderTitle("Start Routine");

        limitForm = SpringContext.getBean(RoutineLimitForm.class);
        limitForm.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep(K.SHORT_SCREEN_WIDTH, 3));

        Shortcuts.addShortcutListener(dialog,
                e -> dialog.close(),
                Key.ESCAPE);
        Shortcuts.addShortcutListener(dialog,
                e -> {if (!limitForm.durationField().isInvalid()) start();},
                Key.ESCAPE);

        Button startButton = new Button("Start", e -> start());
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        startButton.getStyle().set("margin-right", "auto");

        dialog.getFooter().add(startButton, cancelButton);
        dialog.add(limitForm);

        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
        UI.getCurrent().add(dialog);
    }

    public void open(List<PersistedDataDO> data) {
        this.data = data;
        boolean single = data.size() == 1;
        if (!single) dialog.setHeaderTitle("Start Routine from " + data.size() + " tasks");

        Set<Tag> tags = data.stream()
                .map(d -> {
                    if (d instanceof Task task) {
                        if (single) dialog.setHeaderTitle("Start Routine: " + task.name());
                        return task.id();
                    } else if (d instanceof TaskNode node) {
                        if (single) dialog.setHeaderTitle("Start Routine: " + node.name());
                        return node.task().id();
                    }
                    return null;
                })
                .map(id -> controller.services().query().fetchDescendantNodes(id, new TaskNodeTreeFilter()
                        .completed(false))
                        .map(TaskNode::task)
                        .map(Task::id)
                        .toList())
                .flatMap(List::stream)
                .distinct()
                .map(id -> controller.taskNetworkGraph().getTags(id))
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        limitForm.setToggleableTags(tags);

        dialog.open();
    }

    public void start() {
        controller.createRoutine(data, limitForm.getFilter(),
                r -> {
                    UI.getCurrent().navigate(RoutineView.class);
                    dialog.close();
                },
                null);
    }
}
