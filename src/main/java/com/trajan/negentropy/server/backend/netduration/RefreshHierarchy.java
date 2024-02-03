package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.model.interfaces.TaskOrTaskLinkEntity;
import com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.StepRefreshHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;


public interface RefreshHierarchy extends Ancestor<StepRefreshHierarchy> {

    static boolean matchesData(RoutineStepEntity step, TaskOrTaskLinkEntity data) {
        if (step == null) return false;
        return (data instanceof TaskLink link)
                ? (step.link().isPresent() && link.id().equals(step.link().orElseThrow().id()))
                : data.id().equals(step.task().id());
    }

    static boolean matchesData(RoutineStepEntity first, RoutineStepEntity second) {
        if (first == null || second == null) return false;
        TaskOrTaskLinkEntity data = (second.link().isPresent()) ? second.link().get() : second.task();
        return matchesData(first, data);
    }

    static boolean isNotStartedOrSkipped(RoutineStepEntity step) {
        return step.status().equals(TimeableStatus.NOT_STARTED) || step.status().equals(TimeableStatus.SKIPPED);
    }

    default void addToChildren(RoutineStepEntityHierarchy childHierarchy) {
        if (childHierarchy instanceof StepRefreshHierarchy stepRefreshHierarchy) {
            if (stepRefreshHierarchy.customIndexOfChild == null) {
                children().add(stepRefreshHierarchy);
            } else {
                children().add(stepRefreshHierarchy.customIndexOfChild, stepRefreshHierarchy);
            }        } else {
            throw new IllegalArgumentException("Child must be a RefreshHierarchy, is "
                    + childHierarchy.getClass().getSimpleName());
        }
    }

    @Getter
    @Setter
    @Slf4j
    @Accessors(chain = false)
    class RoutineRefreshHierarchy extends RoutineEntityHierarchy implements RefreshHierarchy {
        private Map<LinkID, Duration> stepLinkDurations = new HashMap<>();
        private Map<TaskID, Duration> stepTaskDurations = new HashMap<>();

        private List<StepRefreshHierarchy> children = new ArrayList<>();

        public RoutineRefreshHierarchy(RoutineEntity parent) {
            super(parent);

            TimeableUtil timeableUtil = TimeableUtil.get();
            LocalDateTime now = SpringContext.getBean(RoutineService.class).now();
            routine.descendants().forEach(step -> {
                if (step.link().isPresent()) {
                    stepLinkDurations.put
                            (ID.of(step.link().get()),
                            timeableUtil.getRemainingDurationIncludingLimitExceeded(step, now));
                } else {
                    stepTaskDurations.put(
                            ID.of(step.task()),
                            timeableUtil.getRemainingDurationIncludingLimitExceeded(step, now));
                }
            });
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy hierarchy) {
            log.trace("Adding to routine refresh hierarchy: " + hierarchy.step.task().name() +
                    " with status " + hierarchy.step.status());
            this.addToChildren(hierarchy);
        }

        public Duration getRemainingDuration(TaskOrTaskLinkEntity entity) {
            Duration result = (entity instanceof TaskLink link)
                    ? stepLinkDurations.get(ID.of(link))
                    : stepTaskDurations.get(ID.of((TaskEntity) entity));
            if (result == null && entity instanceof TaskLink link) {
                log.debug("No duration found for link <" + link.child().name() + ">");
                result = stepTaskDurations.get(ID.of(link.child()));
                if (result == null) {
                    log.debug("No duration found for task <" + link.child().name() + "> after link lookup failed");
                    result = link.child().duration();
                }
            }
            return result;
        }

        public RoutineEntity commit() {
            log.debug("Committing routine refresh hierarchy");
            List<RoutineStepEntity> entityDescendants = routine.descendants().stream()
                    .filter(step -> !step.status().isFinished())
                    .toList();
            List<StepRefreshHierarchy> hierarchyDescendants = new ArrayList<>();
            children.forEach(child -> {
                // TODO: Not strictly necessary to filter the hierarchy descendants here
                hierarchyDescendants.addAll(DFSUtil.traverse(child).stream()
                        .filter(hierarchy -> !hierarchy.step.status().isFinished())
                        .toList());
            });

            if (entityDescendants.size() != hierarchyDescendants.size()) {
                log.warn("Mismatched descendant counts; entity descendants size : " + entityDescendants.size()
                        + ", hierarchy descendants size : " + hierarchyDescendants.size());
            }

            LinkedList<RoutineStepEntity> entityDescendantsDeque = new LinkedList<>(entityDescendants);
            LinkedList<StepRefreshHierarchy> hierarchyDescendantsDeque =  new LinkedList<>(hierarchyDescendants);

            // Use deques instead of iterators
            while (!entityDescendantsDeque.isEmpty() && !hierarchyDescendantsDeque.isEmpty()) {
                RoutineStepEntity originalStep = entityDescendantsDeque.peek();
                RoutineStepEntity mirroredStep = hierarchyDescendantsDeque.peek().step();
                log.trace("Comparing <" + originalStep.task().name() + "> to <" + mirroredStep.task().name() + ">");
                if (!matchesData(originalStep, mirroredStep)) {
                    log.warn("Steps not matching: Original step index : " + entityDescendants.indexOf(originalStep) + ", mirrored step index : "
                            + hierarchyDescendants.stream().map(step -> step.step).toList().indexOf(mirroredStep));

                    if (originalStep.link().isEmpty() && mirroredStep.link().isEmpty()) {
                        log.error("Unexpected: both steps have no reference links and do not match. Skipping both");
                        entityDescendantsDeque.pop();
                        hierarchyDescendantsDeque.pop();
                    } else {
                        List<TaskEntity> mirroredTasks = hierarchyDescendantsDeque.stream()
                                .map(hierarchy -> hierarchy.step().task())
                                .toList();
                        List<TaskEntity> originalTasks = entityDescendantsDeque.stream()
                                .map(RoutineStepEntity::task)
                                .toList();

                        if (!mirroredTasks.contains(originalStep.task())) {
                            entityDescendantsDeque.pop();
                        } else if (!originalTasks.contains(mirroredStep.task())) {
                            hierarchyDescendantsDeque.pop();
                        }
                    }

                    continue;
                }

                boolean mirroredStepExceedsLimit = mirroredStep.status().equals(TimeableStatus.LIMIT_EXCEEDED);

                if (!originalStep.status().equals(mirroredStep.status())) {
                    if (mirroredStepExceedsLimit) {
                        if (isNotStartedOrSkipped(originalStep)) {
                            log.debug("Now exceeds limit : <" + originalStep.task().name() + ">");
                            originalStep.status(TimeableStatus.LIMIT_EXCEEDED);
                        }
                    } else if (originalStep.status().equals(TimeableStatus.LIMIT_EXCEEDED)
                            && mirroredStep.status().equals(TimeableStatus.NOT_STARTED)) {
                        log.debug("No longer exceeds limit : <" + originalStep.task().name() + ">");
                        originalStep.status(TimeableStatus.NOT_STARTED);
                    }
                }

                entityDescendantsDeque.pop();
                hierarchyDescendantsDeque.pop();
            }

            return routine;
        }
    }

    @Getter
    @Setter
    @Slf4j
    @Accessors(chain = false)
    class StepRefreshHierarchy extends RoutineStepEntityHierarchy implements RefreshHierarchy {
        private RefreshHierarchy parent;
        private List<StepRefreshHierarchy> children = new ArrayList<>();

        private RoutineRefreshHierarchy routineRefreshHierarchy;

        public StepRefreshHierarchy(TaskOrTaskLinkEntity entity, RoutineEntity routine, RefreshHierarchy parent) {
            super();
            this.step = (entity instanceof TaskLink link)
                    ? new RoutineStepEntity(link)
                    : new RoutineStepEntity((TaskEntity) entity);
            this.step.routine(routine);
            this.parent = parent;

            if (parent instanceof RoutineRefreshHierarchy inputRoutineRefreshHierarchy) {
                this.routineRefreshHierarchy = inputRoutineRefreshHierarchy;
            } else if (parent instanceof StepRefreshHierarchy stepRefreshHierarchy) {
                this.routineRefreshHierarchy = stepRefreshHierarchy.routineRefreshHierarchy;
            }
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy childHierarchy) {
            log.trace("Adding to refresh step hierarchy: " + childHierarchy.step.task().name() + " with status "
                    + childHierarchy.step.status());
            this.addToChildren(childHierarchy);
        }

        public Duration getRemainingDuration(TaskOrTaskLinkEntity entity) {
            return routineRefreshHierarchy.getRemainingDuration(entity);
        }

        @Override
        public String toString() {
            return "StepRefreshHierarchy<" + step.task().name() + ">";
        }
    }
}
