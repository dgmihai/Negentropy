package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.HasTaskNodeData.HasTaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.TaskID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

@NoArgsConstructor
@Getter
@Setter
public class TaskNodeDTO implements TaskNodeDTOData<TaskNodeDTO>, HasTaskNodeDTOData<TaskNodeDTO> {
    private TaskID parentId;
    private TaskID childId;

    private Integer position;
    private Boolean positionFrozen;
    private Integer importance;

    private Boolean completed;
    private Boolean recurring;
    private Boolean cycleToEnd;
    private CronExpression cron;

    @Nullable
    private Optional<Duration> projectDurationLimit;
    @Nullable
    private Optional<Integer> projectStepCountLimit;
    @Nullable
    private Optional<LocalTime> projectEtaLimit;

    public TaskNodeDTO(TaskID parentId, TaskID childId, Integer position, Boolean positionFrozen, Integer importance,
                       Boolean completed, Boolean recurring, Boolean cycleToEnd, CronExpression cron,
                       Optional<Duration> projectDurationLimit, Optional<Integer> projectStepCountLimit,
                       Optional<LocalTime> projectEtaLimit) {
        this.parentId = parentId;
        this.childId = childId;
        this.position = position;
        this.positionFrozen = positionFrozen;
        this.importance = importance;
        this.completed = completed;
        this.recurring = recurring;
        this.cycleToEnd = cycleToEnd;
        this.cron = cron;
        this.projectDurationLimit = projectDurationLimit;
        this.projectStepCountLimit = projectStepCountLimit;
        this.projectEtaLimit = projectEtaLimit;
    }

    public TaskNodeDTO(TaskNodeDTOData<?> node) {
        this.parentId = node.parentId();
        this.childId = node.childId();
        this.position = node.position();
        this.positionFrozen = node.positionFrozen();
        this.importance = node.importance();
        this.completed = node.completed();
        this.recurring = node.recurring();
        this.cycleToEnd = node.cycleToEnd();
        this.cron = node.cron();
        this.projectDurationLimit = node.projectDurationLimit();
        this.projectStepCountLimit = node.projectStepCountLimit();
        this.projectEtaLimit = node.projectEtaLimit();
    }

    public TaskNodeDTO(TaskNodeInfoData<?> node) {
        this.position = node.position();
        this.importance = node.importance();
        this.completed = node.completed();
        this.recurring = node.recurring();
        this.cycleToEnd = node.cycleToEnd();
        this.cron = node.cron();
        this.projectDurationLimit = node.projectDurationLimit();
        this.projectStepCountLimit = node.projectStepCountLimit();
        this.projectEtaLimit = node.projectEtaLimit();
    }

    @Override
    public TaskNodeDTOData<TaskNodeDTO> node() {
        return this;
    }

    public TaskNode toDO() {
        return new TaskNode(
                null,
                parentId,
                null,
                position,
                positionFrozen,
                importance,
                null,
                completed,
                recurring,
                cycleToEnd,
                cron,
                null,
                projectDurationLimit,
                projectStepCountLimit,
                projectEtaLimit);
    }

    @Override
    public TaskNodeDTO toDTO() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TaskNodeDTO(");
        if (parentId != null) result.append("parentId=").append(parentId);
        if (childId != null) result.append(", childId=").append(childId);
        if (position != null) result.append(", position=").append(position);
        if (positionFrozen != null) result.append(", positionFrozen=").append(positionFrozen);
        if (importance != null) result.append(", importance=").append(importance);
        if (completed != null) result.append(", completed=").append(completed);
        if (recurring != null) result.append(", recurring=").append(recurring);
        if (cycleToEnd != null) result.append(", cycleToEnd=").append(cycleToEnd);
        if (cron != null) result.append(", cron=").append(cron);
        if (projectDurationLimit != null) result.append(", projectDurationLimit=").append(projectDurationLimit);
        if (projectStepCountLimit != null) result.append(", projectStepCountLimit=").append(projectStepCountLimit);
        if (projectEtaLimit != null) result.append(", projectEtaLimit=").append(projectEtaLimit);
        result.append(")");
        return result.toString();
    }
}
