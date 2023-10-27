package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Slf4j
public abstract class RoutineStepHierarchy {
    @Setter protected Integer customIndexOfChild = null;

    public abstract void addToHierarchy(RoutineStepEntityHierarchy hierarchy);
    
    protected void finalizeStep(RoutineStepEntity step, RoutineStepHierarchy parent) {
        List<RoutineStepEntity> children = parent.children();
        if (customIndexOfChild != null) {
            step.position(customIndexOfChild);
            children.add(customIndexOfChild, step);
        } else {
            step.position(children.size());
            children.add(step);
        }
    }

    public abstract RoutineEntity routine();

    public abstract List<RoutineStepEntity> children();
    
    @Getter
    @RequiredArgsConstructor
    public static class RoutineEntityHierarchy extends RoutineStepHierarchy {
        private final RoutineEntity parent;

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy hierarchy) {
            log.debug("Adding to hierarchy: " + hierarchy.parent.task().name());
            RoutineStepEntity child = hierarchy.parent();
            finalizeStep(child, this);
            child.routine(parent);
        }

        @Override
        public RoutineEntity routine() {
            return parent;
        }

        @Override
        public List<RoutineStepEntity> children() {
            return parent.children();
        }
    }

    public static RoutineStepEntityHierarchy createRoutineEntityHierarchy(RoutineStepEntityHierarchy hierarchy,
                                                                          TaskLink link, RoutineEntity routine) {
        if (hierarchy == null) return null;
        return (hierarchy instanceof RoutineRecalculationHierarchy)
                ? new RoutineRecalculationHierarchy(link, routine)
                : new RoutineStepEntityHierarchy(link, routine);
    }

    public static RoutineStepEntityHierarchy createRoutineEntityHierarchy(RoutineStepEntityHierarchy hierarchy,
                                                                          TaskEntity task, RoutineEntity routine) {
        if (hierarchy == null) return null;
        return (hierarchy instanceof RoutineRecalculationHierarchy)
                ? new RoutineRecalculationHierarchy(task, routine)
                : new RoutineStepEntityHierarchy(task, routine);
    }

    @Getter
    @RequiredArgsConstructor
    public static class RoutineStepEntityHierarchy extends RoutineStepHierarchy {
        protected final RoutineStepEntity parent;

        public RoutineStepEntityHierarchy(TaskLink link, RoutineEntity routine) {
            this(new RoutineStepEntity(link)
                    .routine(routine));
        }

        public RoutineStepEntityHierarchy(TaskEntity task, RoutineEntity routine) {
            this(new RoutineStepEntity(task)
                    .routine(routine));
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy hierarchy) {
            log.debug("Adding to hierarchy: " + hierarchy.parent.task().name());
            RoutineStepEntity child = hierarchy.parent();
            finalizeStep(child, this);
            child.parentStep(parent);
            child.routine(parent.routine());
        }

        @Override
        public RoutineEntity routine() {
            return parent.routine();
        }

        @Override
        public List<RoutineStepEntity> children() {
            return parent.children();
        }
    }

    @Getter
    public static class RoutineRecalculationHierarchy extends RoutineStepEntityHierarchy {

        public RoutineRecalculationHierarchy(TaskLink link, RoutineEntity routine) {
            super(link, routine);
        }

        public RoutineRecalculationHierarchy(TaskEntity task, RoutineEntity routine) {
            super(task, routine);
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy hierarchy) {
            log.debug("Adding to hierarchy: " + hierarchy.parent.task().name());
            RoutineStepEntity child = hierarchy.parent();
            finalizeStep(child, this);
            child.parentStep(parent);
            child.routine(parent.routine());
        }
    }
}