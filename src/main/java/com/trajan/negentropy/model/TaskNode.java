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
import java.util.Optional;

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
    private Boolean cycleToEnd;
    @JsonSerialize(using = CronExpressionSerializer.class)
    private CronExpression cron;
    private LocalDateTime scheduledFor;

    private Optional<Duration> projectDurationLimit;
    private Optional<Integer> projectStepCountLimit;
    private Optional<LocalTime> projectEtaLimit;

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
        this.cycleToEnd = taskNodeInfoData.cycleToEnd();
    }

    public TaskNode(LinkID linkId, TaskNodeInfoData<?> taskNodeInfoData) {
        this.linkId = linkId;
        this.completed = taskNodeInfoData.completed();
        this.cron = taskNodeInfoData.cron();
        this.importance = taskNodeInfoData.importance();
        this.position = taskNodeInfoData.position();
        this.positionFrozen = taskNodeInfoData.positionFrozen();
        this.projectDurationLimit = taskNodeInfoData.projectDurationLimit();
        this.projectStepCountLimit = taskNodeInfoData.projectStepCountLimit();
        this.projectEtaLimit = taskNodeInfoData.projectEtaLimit();
        this.recurring = taskNodeInfoData.recurring();
        this.cycleToEnd = taskNodeInfoData.cycleToEnd();
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

    private static class DecoratedStringBuilder {
        private final StringBuilder stringBuilder = new StringBuilder();
        private boolean first = true;

        public DecoratedStringBuilder appendIfNotNull(String prefix, Object value) {
            if (value != null) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(prefix).append(value);
            }
            return this;
        }

        public DecoratedStringBuilder append(String string) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(", ");
            }
            stringBuilder.append(string);
            return this;
        }

        public String toString() {
            return stringBuilder.toString();
        }

    }

    @Override
    public String toString() {
        return new DecoratedStringBuilder().append("TaskNode(")
                .appendIfNotNull("linkId=", linkId)
                .appendIfNotNull("parentId=", parentId)
                .appendIfNotNull("child=", child)
                .appendIfNotNull("position=", position)
                .appendIfNotNull("positionFrozen=", positionFrozen)
                .appendIfNotNull("importance=", importance)
                .appendIfNotNull("createdAt=", createdAt)
                .appendIfNotNull("completed=", completed)
                .appendIfNotNull("recurring=", recurring)
                .appendIfNotNull("cycleToEnd=", cycleToEnd)
                .appendIfNotNull("cron=", cron)
                .appendIfNotNull("scheduledFor=", scheduledFor)
                .appendIfNotNull("projectDurationLimit=", projectDurationLimit)
                .appendIfNotNull("projectStepCountLimit=", projectStepCountLimit)
                .appendIfNotNull("projectEtaLimit=", projectEtaLimit)
                .append(")")
                .toString();
    }

    @Override
    public String name() {
        return child.name();
    }
}