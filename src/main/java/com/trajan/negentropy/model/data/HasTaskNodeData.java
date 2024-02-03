package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.TaskNodeLimits;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

public interface HasTaskNodeData extends HasTaskData, MayHaveTaskNodeData {
    TaskNode node();
    default Task task() {
        return node().task();
    }

    default Optional<TaskNode> nodeOptional() {
        return Optional.ofNullable(node());
    }

    interface TaskNodeTemplateData<T extends TaskNodeTemplateData<T>> extends Data {
        T importance(Integer importance);
        Integer importance();
        T skipToChildren(Boolean skipToChildren);
        Boolean skipToChildren();
        T completed(Boolean completed);
        Boolean completed();
        T recurring(Boolean recurring);
        Boolean recurring();
        T cron(CronExpression cron);
        CronExpression cron();
        T cycleToEnd(Boolean cycleToEnd);
        Boolean cycleToEnd();

        default String typeName() {
            return "Task Node";
        }
    }

    interface TaskNodeInfoData<T extends TaskNodeInfoData<T>> extends TaskNodeTemplateData<T> {
        T position(Integer position);
        Integer position();
        T positionFrozen(Boolean positionFrozen);
        Boolean positionFrozen();
        T projectDurationLimit(Optional<Duration> projectDurationLimit);
        @Nullable
        Optional<Duration> projectDurationLimit();
        T projectStepCountLimit(Optional<Integer> projectStepCountLimit);
        @Nullable
        Optional<Integer> projectStepCountLimit();
        T projectEtaLimit(Optional<LocalTime> projectEtaLimit);
        @Nullable
        Optional<LocalTime> projectEtaLimit();
        default TaskNodeLimits limits() {
            return new TaskNodeLimits(projectDurationLimit(), projectEtaLimit(), projectStepCountLimit());
        }
    }

    interface TaskNodeDTOData<T extends TaskNodeDTOData<T>> extends TaskNodeInfoData<T> {
        T parentId(TaskID parentId);
        TaskID parentId();
        TaskID childId();
        TaskNodeDTO toDTO();
    }

    interface HasTaskNodeDTOData<T extends TaskNodeDTOData<T>> {
        TaskNodeDTOData<T> node();
    }
}