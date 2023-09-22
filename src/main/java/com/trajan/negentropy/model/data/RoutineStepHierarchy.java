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

    @Getter
    @RequiredArgsConstructor
    public static class RoutineStepEntityHierarchy extends RoutineStepHierarchy {
        private final RoutineStepEntity parent;

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
}