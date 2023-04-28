//package com.trajan.negentropy.client.routine;
//
//import com.trajan.negentropy.server.entity.Routine;
//import com.trajan.negentropy.server.entity.RoutineStep;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.status.RoutineStatus;
//import com.trajan.negentropy.server.service.RoutineService;
//import exclude.TaskService;
//import com.vaadin.flow.component.AttachEvent;
//import com.vaadin.flow.component.UI;
//import com.vaadin.flow.component.accordion.Accordion;
//import com.vaadin.flow.component.button.Button;
//import com.vaadin.flow.component.html.H1;
//import com.vaadin.flow.component.html.H2;
//import com.vaadin.flow.component.html.H3;
//import com.vaadin.flow.component.icon.Icon;
//import com.vaadin.flow.component.icon.VaadinIcon;
//import com.vaadin.flow.component.orderedlayout.FlexComponent;
//import com.vaadin.flow.component.orderedlayout.FlexLayout;
//import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.component.textfield.TextField;
//import com.vaadin.flow.data.binder.Binder;
//import com.vaadin.flow.shared.Registration;
//import lombok.Getter;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//
//@Getter
//public class Routiner extends VerticalLayout {
//
//    private final RoutineService routineService;
//    private final TaskService taskService;
//
//    private Accordion parentTasksAccordion;
//
//    private H2 activeTaskTitle;
//
//    private H3 activeTaskTimer;
//
//    private Button previousTaskButton;
//
//    private Button nextTaskButton;
//
//    private Button playButton;
//    private Button pauseButton;
//
//
//    private FlexLayout playPauseLayout;
//
//    private Registration timerRegistration;
//
//    private Button skipButton;
//
//    private TextField activeTaskDescription;
//
//    private Binder<Routine> binder = new Binder<>(Routine.class);
//
//    public Routiner(Routine routine, RoutineService routineService, TaskService taskService) {
//        this.routineService = routineService;
//        this.taskService = taskService;
//        initComponents();
//        buildLayout();
//
//        binder.setBean(routine);
//        onIterate();
//    }
//
//    private void initComponents() {
//        parentTasksAccordion = new Accordion();
//        playPauseLayout = new FlexLayout();
//
//        previousTaskButton = new Button(new Icon(VaadinIcon.CHEVRON_LEFT));
//        previousTaskButton.addClickListener(event -> {
//            // TODO: Previous task button callback
//        });
//
//        nextTaskButton = new Button(new Icon(VaadinIcon.CHEVRON_RIGHT));
//        nextTaskButton.addClickListener(event -> {
//            binder.setBean(routineService.completeStep(binder.getBean().getId()));
//            this.onIterate();
//        });
//
//        playButton = new Button(new Icon(VaadinIcon.PLAY));
//        playButton.addClickListener(event -> {
//            binder.setBean(routineService.resumeStep(binder.getBean().getId()));
//            playPauseLayout.replace(playButton, pauseButton);
//        });
//
//        pauseButton = new Button(new Icon(VaadinIcon.PAUSE));
//        pauseButton.addClickListener(event -> {
//            binder.setBean(routineService.suspendStep(binder.getBean().getId()));
//            playPauseLayout.replace(pauseButton, playButton);
//        });
//        playPauseLayout.add(pauseButton);
//
//        skipButton = new Button(new Icon(VaadinIcon.CLOSE));
//        skipButton.addClickListener(event -> {
//            binder.setBean(routineService.skipStep(binder.getBean().getId()));
//            this.onIterate();
//        });
//
//        activeTaskDescription = new TextField();
//        binder.forField(activeTaskDescription).bind(
//                routine -> routine.getCurrentStep().getTask().getDescription(),
//                (routine, text) -> {
//                    Task task = routine.getCurrentStep().getTask();
//                    task.setDescription(text);
//                });
//        activeTaskDescription.addValueChangeListener(event -> {
//            RoutineStep step = binder.getBean().getCurrentStep();
//            step.setTask(taskService.updateTask(step.getTask()));
//        });
//
//        activeTaskTitle = new H2("TITLE");
//        activeTaskTimer = new H3("TIMER");
//    }
//
//    private void onIterate() {
//        Routine routine = binder.getBean();
//        if(routine.getStatus() == RoutineStatus.COMPLETED) {
//            this.removeAll();
//            this.add(new H1("COMPLETED!"));
//            this.addClickListener(e -> this.removeFromParent());
//        } else {
//            activeTaskTitle.setText(routine.getCurrentStep().getTask().getTitle());
//            activeTaskTimer.setText(formatDuration(getCurrentTaskDuration(routine.getCurrentStep())));
//        }
//    }
//
//    private void buildLayout() {
//        HorizontalLayout upperTaskControls = new HorizontalLayout(previousTaskButton, activeTaskTimer, nextTaskButton);
//        upperTaskControls.setAlignItems(FlexComponent.Alignment.CENTER);
//        upperTaskControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
//        upperTaskControls.setWidthFull();
//
//        HorizontalLayout lowerTaskControls = new HorizontalLayout(playPauseLayout, skipButton);
//        lowerTaskControls.setAlignItems(FlexComponent.Alignment.CENTER);
//        lowerTaskControls.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
//        lowerTaskControls.setWidthFull();
//
//        VerticalLayout activeTaskLayout = new VerticalLayout(activeTaskTitle, upperTaskControls, lowerTaskControls);
//        activeTaskLayout.setAlignItems(FlexComponent.Alignment.CENTER);
//        activeTaskLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
//        activeTaskLayout.setWidthFull();
//
//        add(parentTasksAccordion, activeTaskLayout, activeTaskDescription);
//
//        setSizeFull();
//        expand(activeTaskLayout);
//        setAlignItems(Alignment.CENTER);
//    }
//
//    private Duration getCurrentTaskDuration(RoutineStep routineStep) {
//        Duration elapsed = routineStep.getElapsedActiveTime();
//        if (routineStep.getLastResumed() != null) {
//            elapsed = elapsed.plus(Duration.between(routineStep.getLastResumed(), LocalDateTime.now()));
//        }
//        return elapsed;
//    }
//
//    private String formatDuration(Duration duration) {
//        long seconds = duration.getSeconds();
//        long absSeconds = Math.abs(seconds);
//        String positive = String.format("%02d:%02d", absSeconds / 60, absSeconds % 60);
//        return seconds < 0 ? "-" + positive : positive;
//    }
//
//    @Override
//    protected void onAttach(AttachEvent attachEvent) {
//        super.onAttach(attachEvent);
//
//        // Update timer periodically
//        UI ui = getUI().orElse(null);
//        if (ui != null) {
//            ui.setPollInterval(1000);
//            ui.addPollListener(event -> {
//                Routine routine = binder.getBean();
//                if (routine.getStatus() == RoutineStatus.ACTIVE) {
//                    RoutineStep step = routine.getCurrentStep();
//                    step.setElapsedActiveTime(step.getElapsedActiveTime());
//                    activeTaskTimer.setText(formatDuration(getCurrentTaskDuration(step)));
//                }
//            });
//        }
//    }
//}
