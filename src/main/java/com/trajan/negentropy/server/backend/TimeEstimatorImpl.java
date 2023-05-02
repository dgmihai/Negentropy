//package com.trajan.negentropy.server.service;
//
//import com.trajan.negentropy.server.entity.TaskEntity;
//import com.trajan.negentropy.server.entity.TaskLink;
//import com.trajan.negentropy.server.entity.TimeEstimate;
//import com.trajan.negentropy.server.events.DataChangeEvent;
//import com.trajan.negentropy.server.model.Task;
//import com.trajan.negentropy.server.model.TaskLink;
//import com.trajan.negentropy.server.repository.TimeEstimateRepository;
//import com.trajan.negentropy.server.task.TaskEntityQueryService;
//import jakarta.transaction.Transactional;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Component
//@Transactional
//public class TimeEstimatorImpl implements TimeEstimator {
//    private static final Logger logger = LoggerFactory.getLogger(TimeEstimatorImpl.class);
//
//    @Autowired
//    TaskEntityQueryService queryService;
//
//    @Autowired
//    TimeEstimateRepository timeEstimateRepository;
//
//    @Override
//    public void onLinkCreated(TaskLink link) {
//        logger.debug("Adding time estimate for each ancestor of new link " + link.child());
//        Duration difference = timeEstimateRepository.findByTaskIdOrderByPriorityAsc(link.child().id()).get(0).duration();
//        this.updateTaskTimeEstimateBy(link.parent(), difference);
//    }
//
//    @Override
//    public void onLinkDeleted(TaskLink link) {
//        logger.debug("Subtracting time estimate for each ancestor of deleted link " + link.child());
//        Duration difference = timeEstimateRepository.findByTaskIdOrderByPriorityAsc(link.child().id()).get(0).duration();
//        this.updateTaskTimeEstimateBy(link.parent(), difference.negated());
//    }
//
//    private void adjustEstimateForTask(long id, Duration difference) {
//        logger.debug("Difference to adjust be is " + difference);
//        List<TimeEstimate> estimates = timeEstimateRepository.findByTaskIdOrderByPriorityAsc(id);
//        TimeEstimate estimate = estimates.get(0);
//
//        logger.debug("Existing time estimate: " + estimate.duration());
//        estimate.duration(estimate.duration().plus(difference));
//        logger.debug("New time estimate: " + estimate.duration());
//    }
//
//    private void onLinksDeleted(Set<TaskLink> nodes) {
//        logger.debug("Adjusting time estimates for " + nodes.size() + " deleted nodes");
//        nodes.forEach(this::onLinkDeleted);
//    }
//
//    @Override
//    public void initTaskTimeEstimate(TaskEntity task) {
//
//    }
//
//    @Override
//    public void onTaskDeleted(TaskEntity task) {
//        logger.debug("Adjusting time estimate for deleted task " + task);
//        this.onLinksDeleted(queryService.getAncestorLinks(task).collect(Collectors.toSet()));
//        timeEstimateRepository.deleteAllByTask(task);
//    }
//
//    @Override
//    public void updateTaskTimeEstimateBy(long taskId, Duration difference) {
//        logger.debug("Task id: " + taskId + " duration updated by " + difference + ", adjusting time estimates");
//        this.updateAncestorTimeEstimateBy(taskId, difference);
//        this.adjustEstimateForTask(taskId, difference);
//    }
//
//    @Override
//    public void updateAncestorTimeEstimateBy(long taskId, Duration difference) {
//        logger.debug("Task " + taskId + " duration updated by " + difference + ", adjusting time estimates for ancestors");
//        queryService.getAncestors(taskId).forEach(ancestor ->
//                this.adjustEstimateForTask(ancestor, difference));
//    }
//
//    @EventListener
//    public void onTaskCreated(DataChangeEvent.Create<TaskEntity> event) {
//        TaskEntity task = event.created();
//        logger.debug("Creating new time estimate for " + task);
//        // TODO: Check for existing time estimates
//        TimeEstimate estimate = new TimeEstimate(task, 0, task.duration());
//        logger.debug("New time estimate: " + estimate.duration());
//        timeEstimateRepository.save(estimate);
//    }
//
//    @EventListener
//    public void onTaskUpdated(DataChangeEvent.Update<TaskEntity, Task> event) {
//        Task updated = event.updated();
//        if (updated.duration() != null) {
//            TaskEntity original = event.original();
//
//            Duration difference = updated.duration().minus(original.duration());
//
//            this.updateTaskTimeEstimateBy(updated.id(), difference);
//        }
//    }
//
//    @EventListener
//    public void onTaskDeleted(DataChangeEvent.Delete<TaskEntity> event) {
//        TaskEntity task = event.deleted();
//
//        this.updateAncestorTimeEstimateBy(task, timeEstimateRepository.findByTaskIdOrderByPriorityAsc(
//                task).get(0).duration().negated());
//
//        timeEstimateRepository.deleteAllByTask(task);
//    }
//
//    @EventListener
//    public void onLinkCreated(DataChangeEvent.Create<TaskLink> event) {
//        TaskEntity parent = event.created().parent();
//        TaskEntity child = event.created().child();
//
//        this.updateTaskTimeEstimateBy(parent, timeEstimateRepository.findByTaskIdOrderByPriorityAsc(
//                child).get(0).duration());
//    }
//
//    @EventListener
//    public void onLinkDeleted(DataChangeEvent.Delete<TaskLink> event) {
//        TaskEntity parent = event.deleted().parent();
//        TaskEntity child = event.deleted().child();
//
//        this.updateTaskTimeEstimateBy(parent, timeEstimateRepository.findByTaskIdOrderByPriorityAsc(
//                child).get(0).duration().negated());
//    }
//
//    @EventListener
//    public void onLinkUpdated(DataChangeEvent.Update<TaskLink, TaskLink> event) {
//        TaskEntity parent = event.updated().parent();
//        TaskEntity child = event.updated().child();
//    }
//
//
//
//
//
//}
