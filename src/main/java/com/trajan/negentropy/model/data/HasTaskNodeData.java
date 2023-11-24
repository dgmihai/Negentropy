package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.id.TaskID;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

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
        T completed(Boolean completed);
        Boolean completed();
        T recurring(Boolean recurring);
        Boolean recurring();
        T cron(CronExpression cron);
        CronExpression cron();

        default String typeName() {
            return "Task Node";
        }
    }

    interface TaskNodeInfoData<T extends TaskNodeInfoData<T>> extends TaskNodeTemplateData<T> {
        T position(Integer position);
        Integer position();
        T positionFrozen(Boolean positionFrozen);
        Boolean positionFrozen();
        T projectDurationLimit(Duration projectDurationLimit);
        Duration projectDurationLimit();
        T projectStepCountLimit(Integer projectStepCountLimit);
        Integer projectStepCountLimit();
        T projectEtaLimit(LocalTime projectEtaLimit);
        LocalTime projectEtaLimit();
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

    interface HasTaggedTaskNodeData extends HasTaskNodeData {
        @Override
        TaskDTO task();

        Set<Tag> tags();
    }
}