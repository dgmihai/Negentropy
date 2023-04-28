//package com.trajan.negentropy.client.routine;
//
//import com.trajan.negentropy.client.MainLayout;
//import com.trajan.negentropy.server.entity.Routine;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.status.RoutineStatus;
//import com.trajan.negentropy.server.service.RoutineService;
//import com.trajan.negentropy.server.service.TagService;
//import exclude.TaskService;
//import com.vaadin.flow.component.orderedlayout.FlexLayout;
//import com.vaadin.flow.component.orderedlayout.VerticalLayout;
//import com.vaadin.flow.router.PageTitle;
//import com.vaadin.flow.router.Route;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import java.util.LinkedList;
//
//@Route(value = "routine", layout = MainLayout.class)
//@PageTitle("Routine | Negentropy")
//public class RoutineView extends VerticalLayout {
//    private final RoutinePresenter presenter;
//    private final TaskService taskService;
//    private final TagService tagService;
//    private final RoutineService routineService;
//
//    private LinkedList<Routiner> routiners = new LinkedList<>();
//    private RoutineSelector routineSelector;
//
//    private FlexLayout body;
//
//    public RoutineView(@Autowired RoutinePresenter presenter, TaskService taskService, TagService tagService, RoutineService routineService) {
//        this.presenter = presenter;
//        this.taskService = taskService;
//        this.tagService = tagService;
//        this.routineService = routineService;
//
//        initComponents();
//    }
//
//    private void initComponents() {
//        for (Routine routine : routineService.findRoutinesByStatus(RoutineStatus.ACTIVE)) {
//            this.addRoutiner(new Routiner(routine, routineService, taskService));
//        }
//        routineSelector = new RoutineSelector(this.tagService, this.taskService);
//        this.add(routineSelector);
//
//        this.routineSelector.getStartRoutineButton().addClickListener(event -> {
//            Task task = this.routineSelector.getTaskGrid().getSelectedItems().iterator().next();
//            this.addRoutiner(new Routiner(
//                    routineService.startNewRoutine(task.getId()),
//                    routineService,
//                    taskService));
//        });
//    }
//
//    private void addRoutiner(Routiner routiner) {
//        routiners.add(routiner);
//        this.add(routiners.getLast());
//    }
//}
