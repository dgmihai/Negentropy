package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class RoutineStepHierarchy {
    public abstract void addToHierarchy(RoutineStepEntityHierarchy hierarchy);
    
    protected void finalizeStep(RoutineStepEntity step, RoutineStepHierarchy parent) {
        List<RoutineStepEntity> children = parent.children();
        step.position(children.size());
        children.add(step);
    }

    public abstract RoutineEntity routine();

    public abstract List<RoutineStepEntity> children();
    
    @Getter
    @AllArgsConstructor
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
    @AllArgsConstructor
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