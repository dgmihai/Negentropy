package com.trajan.negentropy.client.view;

import com.trajan.negentropy.client.MainLayout;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.TaskSession;
import com.trajan.negentropy.server.service.RoutineService;
import com.trajan.negentropy.server.service.TaskService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Route(value = "routine", layout = MainLayout.class)
@PageTitle("Negentropy")
public class RoutineView extends VerticalLayout {

    private final RoutineService routineService;
    private final TaskService taskService;
    private TaskSession activeSession;
    private LocalDateTime startTime;
    private TextArea description;
    private Div timerDiv;
    private Button pauseButton;
    private Button completeButton;
    private Button skipButton;
    private boolean isPaused = false;


    public RoutineView(RoutineService routineService, TaskService taskService) {
        this.routineService = routineService;
        this.taskService = taskService;

        initializeView();
    }

    private void initializeView() {
        TaskSession activeTaskSession = routineService.getActiveTaskSession();
        if (activeTaskSession != null) {
            addParentTaskSubheading();
            addCurrentTaskSection();
            addControls();
            UI.getCurrent().addPollListener(event -> updateTimer());
            UI.getCurrent().setPollInterval(1000);
        } else {
            displayTaskSelectionUI();
        }
    }

    private void displayTaskSelectionUI() {
        List<TaskNode> orphanNodes = taskService.findOrphanNodes();
        if (!orphanNodes.isEmpty()) {
            add(new H2("Select a Task to Start"));
            orphanNodes.forEach(node -> {
                Button taskButton = new Button(node.getReferenceTask().getTitle(), event -> startFromNode(node));
                add(taskButton);
            });
        } else {
            add(new Paragraph("No tasks available."));
        }
    }

    public void startFromNode(TaskNode node) {
        removeAll();
        addParentTaskSubheading();
        addCurrentTaskSection();
        addControls();
        UI.getCurrent().addPollListener(event -> updateTimer());
        UI.getCurrent().setPollInterval(1000);
        activeSession = routineService.startTask(node.getId());
    }

    private void addParentTaskSubheading() {
        if (activeSession.getNode().getParentTask() != null) {
            add(new H2(activeSession.getNode().getParentTask().getTitle()));
        }
    }

    private void addCurrentTaskSection() {
        Task currentTask = activeSession.getNode().getReferenceTask();
        add(new H2(currentTask.getTitle()));

        description = new TextArea("Description", currentTask.getDescription());
        description.addValueChangeListener(event -> {
            currentTask.setDescription(event.getValue());
            taskService.updateTask(currentTask);
        });
        add(description);

        startTime = LocalDateTime.now();
        timerDiv = new Div();
        updateTimer();
        add(timerDiv);
    }


    private void addControls() {
        pauseButton = new Button("Pause", event -> togglePause());
        completeButton = new Button("Complete", event -> completeTask());
        skipButton = new Button("Skip", event -> skipTask());

        HorizontalLayout controlsLayout = new HorizontalLayout(pauseButton, completeButton, skipButton);
        add(controlsLayout);
    }

    private void togglePause() {
        isPaused = !isPaused;
        long taskId = activeSession.getNode().getReferenceTask().getId();
        if (isPaused) {
            routineService.pauseTask(taskId);
        } else {
            routineService.resumeTask(taskId);
        }
        pauseButton.setText(isPaused ? "Resume" : "Pause");
    }

    private void completeTask() {
        routineService.completeTask(activeSession.getId());
        TaskNode node = activeSession.getNode().getNext();
        if (node != null) {
            routineService.startTask(node.getId());
            updateViewForNextTask();
        } else {
            showRoutineCompleted();
        }
    }

    private void skipTask() {
        Dialog skipDialog = new Dialog();
        TextField explanationField = new TextField("Why?");
        Button submitButton = new Button("Submit", event -> {
            // Process the explanation if necessary, e.g., save to the backend.
            skipDialog.close();
            completeTask(); // Proceed to the next task after submitting the explanation.
        });
        skipDialog.add(explanationField, submitButton);
        skipDialog.open();
    }

    private void updateViewForNextTask() {
        insertChildTasksToQueueIfNeeded();

        removeAll();
        if (activeSession != null) {
            addParentTaskSubheading();
            addCurrentTaskSection();
            addControls();
        } else {
            showRoutineCompleted();
        }
    }

    private void insertChildTasksToQueueIfNeeded() {
        long taskId = activeSession.getNode().getReferenceTask().getId();
        int childCount = taskService.countChildNodes(taskId);

        if (childCount > 0) {
            List<TaskNode> childNodes = taskService.findChildNodes(taskId);
            TaskNode lastNodeInList = activeSession.getNode();

            while (lastNodeInList.getNext() != null) {
                lastNodeInList = lastNodeInList.getNext();
            }

            lastNodeInList.setNext(childNodes.get(0));
            childNodes.get(0).setPrev(lastNodeInList);
        }
    }

    private void showRoutineCompleted() {
        add(new Paragraph("Routine completed."));
    }

    private void updateTimer() {
        if (!isPaused) {
            Duration elapsedTime = Duration.between(startTime, LocalDateTime.now());
            String formattedElapsedTime = formatDuration(elapsedTime);
            timerDiv.setText("Elapsed Time: " + formattedElapsedTime);
        }
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);
        String positive = String.format(
                "%02d:%02d:%02d",
                absSeconds / 3600,
                (absSeconds % 3600) / 60,
                absSeconds % 60);

        return seconds < 0 ? "-" + positive : positive;
    }

}
