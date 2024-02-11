package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineEntityHierarchy;
import com.trajan.negentropy.server.backend.netduration.RoutineStepHierarchy.RoutineStepEntityHierarchy;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

public interface RefreshHierarchy {

    private boolean canBeCheckedForRefresh(RoutineStepEntity step) {
        return (step.status().equals(TimeableStatus.NOT_STARTED)
                || step.status().equals(TimeableStatus.SKIPPED)
                || step.status().equals(TimeableStatus.LIMIT_EXCEEDED));
    }

    default void addToHierarchy(RoutineStepEntityHierarchy childHierarchy) {
        if (canBeCheckedForRefresh(childHierarchy.step)) {
            if (childHierarchy.exceedsLimit()) {
                childHierarchy.step.status(TimeableStatus.LIMIT_EXCEEDED);
            } else {
                childHierarchy.step.status(TimeableStatus.NOT_STARTED);
            }
        }
    }

    @Getter
    @Setter
    @Slf4j
    @Accessors(chain = false)
    class RoutineRefreshHierarchy extends RoutineEntityHierarchy implements RefreshHierarchy {
        private List<StepRefreshHierarchy> children = new ArrayList<>();

        public RoutineRefreshHierarchy(RoutineEntity parent) {
            super(parent);
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy childHierarchy) {
            log.trace("Adding <" + childHierarchy.step.name() + "> to refresh hierarchy with status "
                    + childHierarchy.step.status());
            RefreshHierarchy.super.addToHierarchy(childHierarchy);
        }
    }

    @Getter
    @Setter
    @Slf4j
    @Accessors(chain = false)
    class StepRefreshHierarchy extends RoutineStepEntityHierarchy implements RefreshHierarchy {
        public StepRefreshHierarchy(RoutineStepEntity step) {
            super();
            this.step = step;
        }

        @Override
        public void addToHierarchy(RoutineStepEntityHierarchy childHierarchy) {
            log.trace("Adding <" + childHierarchy.step.name() + "> to refresh hierarchy with status "
                    + childHierarchy.step.status());
            RefreshHierarchy.super.addToHierarchy(childHierarchy);
        }

        @Override
        public String toString() {
            return "StepRefreshHierarchy<" + step.task().name() + ">";
        }
    }
}
