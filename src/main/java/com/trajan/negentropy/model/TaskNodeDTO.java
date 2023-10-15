package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.HasTaskNodeData.HasTaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import com.trajan.negentropy.model.id.TaskID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;

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
    private CronExpression cron;

    private Duration projectDuration;

    public TaskNodeDTO(TaskID parentId, TaskID childId, Integer position, Boolean positionFrozen, Integer importance,
                       Boolean completed, Boolean recurring, CronExpression cron, Duration projectDuration) {
        this.parentId = parentId;
        this.childId = childId;
        this.position = position;
        this.positionFrozen = positionFrozen;
        this.importance = importance;
        this.completed = completed;
        this.recurring = recurring;
        this.cron = cron;
        this.projectDuration = projectDuration;
    }

    public TaskNodeDTO(TaskNodeDTOData<?> node) {
        this.parentId = node.parentId();
        this.childId = node.childId();
        this.position = node.position();
        this.positionFrozen = node.positionFrozen();
        this.importance = node.importance();
        this.completed = node.completed();
        this.recurring = node.recurring();
        this.cron = node.cron();
        this.projectDuration = node.projectDuration();
    }

    public TaskNodeDTO(TaskNodeInfoData<?> node) {
        this.position = node.position();
        this.importance = node.importance();
        this.completed = node.completed();
        this.recurring = node.recurring();
        this.cron = node.cron();
        this.projectDuration = node.projectDuration();
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
                cron,
                null,
                projectDuration);
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
        if (cron != null) result.append(", cron=").append(cron);
        if (projectDuration != null) result.append(", projectDuration=").append(projectDuration);
        result.append(")");
        return result.toString();
    }
}
