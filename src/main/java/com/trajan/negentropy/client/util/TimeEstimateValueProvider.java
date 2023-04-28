//package com.trajan.negentropy.client.util;
//
//import com.trajan.negentropy.client.TaskEntry;
//import com.trajan.negentropy.server.entity.Task;
//import exclude.TaskService;
//import com.vaadin.flow.function.ValueProvider;
//import com.vaadin.flow.spring.annotation.UIScope;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Locale;
//
//@UIScope
//public record TimeEstimateValueProvider<T>(ToggleButton toggleButton, TaskService taskService) implements ValueProvider<T, String> {
//    @Override
//    public String apply(T obj) {
//        if (obj instanceof Task task) {
//            return convert(task);
//        } else if (obj instanceof TaskEntry entry) {
//            return convert(entry.link().getReferenceTask());
//        } else {
//            return "Invalid source type.";
//        }
//    }
//
//    private String convert(Task task) {
//        int children = taskService.countChildNodes(task.getId());
//        Duration duration = taskService.getTimeEstimate(task.getId());
//        if (children == 0) {
//            duration = task.getDuration();
//            if (duration.isZero()) {
//                return "";
//            }
//        }
//        if (toggleButton.isToggled()) {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
//            return LocalDateTime.now().plus(duration).format(formatter);
//        } else {
//            return new DurationConverter().convertToPresentation(duration, null);
//        }
//    }
//}
