package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.netduration.RefreshHierarchy.StepRefreshHierarchy;
import com.trajan.negentropy.util.SpringContext;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

@Getter
@Slf4j
public abstract class RoutineStepHierarchy {
    @Setter protected Integer customIndexOfChild = null;

    public static RoutineEntity integrateTaskOrTaskLinkIntoRoutineStep(Integer index, TaskNodeTreeFilter filter,
                                                              RoutineStepEntity root, HasTaskLinkOrTaskEntity child) {
        return integrateTaskOrTaskLinkIntoHierarchy(index, filter, new RoutineStepEntityHierarchy(root), child);
    }

    public static RoutineEntity integrateTaskOrTaskLinkIntoRoutineAsRoot(Integer index, TaskNodeTreeFilter filter,
                                                                             RoutineEntity root, HasTaskLinkOrTaskEntity child) {
        return integrateTaskOrTaskLinkIntoHierarchy(index, filter, new RoutineEntityHierarchy(root), child);
    }

    private static RoutineEntity integrateTaskOrTaskLinkIntoHierarchy(Integer index, TaskNodeTreeFilter filter,
                                                 RoutineStepHierarchy hierarchy, HasTaskLinkOrTaskEntity child) {
        hierarchy.customIndexOfChild = index;
        NetDurationService netDurationService = SpringContext.getBean(NetDurationService.class);
        NetDurationHelper helper = netDurationService.getHelper(filter);
        if (child instanceof TaskLink link) {
            if (hierarchy.exceedsLimit()) {
                helper.loadAdjacencyMap();
                helper.linkHierarchyIterator().iterateAsExceededLimit(link, hierarchy, false);
            } else {
                helper.calculateHierarchicalNetDuration(link, hierarchy, null);
            }
        } else if (child instanceof TaskEntity task) {
            if (hierarchy.exceedsLimit()) {
                helper.loadAdjacencyMap();
                helper.linkHierarchyIterator().iterateAsExceededLimit(task, hierarchy, false);
            } else {
                helper.calculateHierarchicalNetDuration(task, hierarchy, null);
            }
        } else throw new IllegalArgumentException("Child must be a TaskEntity or TaskLink");

        return hierarchy.routine();
    }

    public abstract void addToHierarchy(RoutineStepEntityHierarchy hierarchy);

    protected void finalizeStep(RoutineStepEntity step, RoutineStepHierarchy data) {
        if (data.exceedsLimit()) step.status(TimeableStatus.LIMIT_EXCEEDED);
        List<RoutineStepEntity> children = data.childSteps();
        if (customIndexOfChild != null) {
            step.position(customIndexOfChild);
            children.add(customIndexOfChild, step);
        } else {
            step.position(children.size());
            children.add(step);
        }
    }

    public abstract RoutineEntity routine();

    public abstract List<RoutineStepEntity> childSteps();

    public abstract boolean exceedsLimit();

    public abstract void setExceedsLimit();

    @Getter
    @RequiredArgsConstructor
    public static class RoutineEntityHierarchy extends RoutineStepHierarchy {
        protected final RoutineEntity routine;

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy hierarchy) {
            log.trace("Adding to routine hierarchy: " + hierarchy.step.task().name() + " with status " + hierarchy.step.status());
            RoutineStepEntity child = hierarchy.step();
            finalizeStep(child, this);
            child.routine(routine);
        }

        @Override
        public List<RoutineStepEntity> childSteps() {
            return routine.children();
        }

        @Override
        public boolean exceedsLimit() {
            return routine.status().equals(TimeableStatus.LIMIT_EXCEEDED);
        }

        @Override
        public void setExceedsLimit() {
            throw new IllegalArgumentException("Cannot set limit exceeded on routine hierarchy");
        }
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoutineStepEntityHierarchy extends RoutineStepHierarchy {
        protected RoutineStepEntity step;

        public static RoutineStepEntityHierarchy create(HasTaskLinkOrTaskEntity entity, RoutineEntity routine, RoutineStepHierarchy parent) {
            return (entity instanceof RoutineStepEntity step)
                    ? new StepRefreshHierarchy(step)
                    : new RoutineStepEntityHierarchy(entity, routine);
        }

        public RoutineStepEntityHierarchy(HasTaskLinkOrTaskEntity entity, RoutineEntity routine) {
            if (entity instanceof TaskLink link) {
                this.step = new RoutineStepEntity(link);
            } else if (entity instanceof TaskEntity task) {
                this.step = new RoutineStepEntity(task);
            } else {
                throw new IllegalArgumentException("Data entity must be a TaskEntity or TaskLink");
            }
            step.routine(routine);
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy child) {
            log.debug("Adding to step hierarchy: " + child.step.task().name() + " with status " + child.step.status());
            RoutineStepEntity childStep = child.step();
            finalizeStep(childStep, this);
            childStep.parentStep(step);
            childStep.routine(step.routine());
        }

        @Override
        public RoutineEntity routine() {
            return step.routine();
        }

        @Override
        public List<RoutineStepEntity> childSteps() {
            return step.children();
        }

        @Override
        public boolean exceedsLimit() {
            return step.status().equals(TimeableStatus.LIMIT_EXCEEDED);
        }

        @Override
        public void setExceedsLimit() {
            if (Set.of(
                    TimeableStatus.NOT_STARTED,
                    TimeableStatus.SKIPPED)
                    .contains(step.status())) {
                step.status(TimeableStatus.LIMIT_EXCEEDED);
            }
        }
    }
}