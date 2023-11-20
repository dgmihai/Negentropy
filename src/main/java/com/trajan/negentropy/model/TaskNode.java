package com.trajan.negentropy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskNodeData;
import com.trajan.negentropy.model.data.HasTaskNodeData.HasTaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.util.CronExpressionSerializer;
import lombok.*;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter(onMethod_={@JsonProperty})
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class TaskNode implements TaskNodeDTOData<TaskNode>, HasTaskNodeData, HasTaskNodeDTOData<TaskNode>, PersistedDataDO<LinkID>, Comparable<TaskNode> {
    @EqualsAndHashCode.Include
    private final LinkID linkId;

    private TaskID parentId;
    private Task child;
    private Integer position;
    private Boolean positionFrozen;
    private Integer importance;

    private LocalDateTime createdAt;
    private Boolean completed;

    private Boolean recurring;
    @JsonSerialize(using = CronExpressionSerializer.class)
    private CronExpression cron;
    private LocalDateTime scheduledFor;

    private Duration projectDurationLimit;
    private Integer projectStepCountLimit;
    private LocalTime projectEtaLimit;

    public TaskNode(TaskNodeInfoData<?> taskNodeInfoData) {
        this.linkId = null;
        this.completed = taskNodeInfoData.completed();
        this.cron = taskNodeInfoData.cron();
        this.importance = taskNodeInfoData.importance();
        this.position = taskNodeInfoData.position();
        this.positionFrozen = taskNodeInfoData.positionFrozen();
        this.projectDurationLimit = taskNodeInfoData.projectDurationLimit();
        this.projectStepCountLimit = taskNodeInfoData.projectStepCountLimit();
        this.projectEtaLimit = taskNodeInfoData.projectEtaLimit();
        this.recurring = taskNodeInfoData.recurring();
    }

    public TaskNode(LinkID linkId, TaskNodeInfoData<?> taskNodeInfoData) {
        this.linkId = linkId;
        this.completed = taskNodeInfoData.completed();
        this.cron = taskNodeInfoData.cron();
        this.importance = taskNodeInfoData.importance();
        this.position = taskNodeInfoData.position();
        this.positionFrozen = taskNodeInfoData.positionFrozen();
        this.projectDurationLimit = taskNodeInfoData.projectDurationLimit();
        this.recurring = taskNodeInfoData.recurring();
    }

    @Override
    public TaskNode node() {
        return this;
    }

    @Override
    public Task task() {
        return child;
    }

    @Override
    public LinkID id() {
        return linkId;
    }

    @Override
    public int compareTo(TaskNode other) {
        if (!Objects.equals(parentId, other.parentId)) {
            throw new IllegalArgumentException("Cannot compare TaskNodes with different parents");
        }

        return Integer.compare(position, other.position);
    }

    @Override
    public TaskID childId() {
        return child.id();
    }

    public TaskNodeDTO toDTO() {
        return new TaskNodeDTO(this);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("TaskNode(");
        result.append("linkId=").append(linkId);

        if (parentId != null) result.append(", parentId=").append(parentId);
        if (child != null) result.append(", child=").append(child);
        if (position != null) result.append(", position=").append(position);
        if (positionFrozen != null) result.append(", positionFrozen=").append(positionFrozen);
        if (importance != null) result.append(", importance=").append(importance);
        if (createdAt != null) result.append(", createdAt=").append(createdAt);
        if (completed != null) result.append(", completed=").append(completed);
        if (recurring != null) result.append(", recurring=").append(recurring);
        if (cron != null) result.append(", cron=").append(cron);
        if (scheduledFor != null) result.append(", scheduledFor=").append(scheduledFor);
        if (projectDurationLimit != null) result.append(", projectDurationLimit=").append(projectDurationLimit);

        result.append(")");
        return result.toString();
    }

    @Override
    public String name() {
        return child.name();
    }
}