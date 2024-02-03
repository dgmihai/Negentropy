package com.trajan.negentropy.model.entity;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.Descendant;
import com.trajan.negentropy.model.interfaces.HasDuration;
import com.trajan.negentropy.model.interfaces.TaskOrTaskLinkEntity;
import com.trajan.negentropy.server.backend.sync.SyncManagerListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Entity
@EntityListeners(SyncManagerListener.class)
@Table(name = "task_links", indexes = {
        @Index(columnList = "parent_id", name = "idx_link_parent"),
        @Index(columnList = "scheduled_for", name = "idx_link_scheduled_for"),
        @Index(columnList = "completed", name = "idx_link_completed"),
        @Index(columnList = "recurring", name = "idx_link_recurring"),
        @Index(columnList = "completed, scheduled_for, recurring", name = "idx_link_filter")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Slf4j
public class TaskLink extends AbstractEntity implements Descendant<TaskEntity>, TaskNodeDTOData<TaskLink>, HasDuration,
        TaskOrTaskLinkEntity {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "task_link_seq", sequenceName = "task_link_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_link_seq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TaskEntity parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    @NotNull
    private TaskEntity child;

    private Integer position = 0;
    private Boolean positionFrozen = false;
    private Boolean skipToChildren = false;
    private Integer importance = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    @Setter(AccessLevel.PRIVATE)
    private LocalDateTime completedAt;
    private Boolean completed = false;

    private Boolean recurring = false;
    private Boolean cycleToEnd = false;
    private String cron;
    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor = null;

    private Duration projectDurationLimit;
    private Integer projectStepCountLimit;
    private String projectEtaLimit;

    public String toString() {
        String parent = (this.parent != null) ? this.parent.name() : "null";
        return "LinkEntity(" + id + ")[name=" + child.name() + ", parent=" + parent + ", position=" + position
                + ", positionFrozen=" + positionFrozen + ", skipToChildren=" + skipToChildren + ", created at=" + createdAt
                + ", completed=" + completed + ", recurring=" + recurring + ", cycleToEnd=" + cycleToEnd + ", cron="
                + cron + ", scheduledFor=" + scheduledFor + ", projectDurationLimit=" + projectDurationLimit
                + ", projectStepCountLimit=" + projectStepCountLimit + ", projectEtaLimit=" + projectEtaLimit + "]";
    }

    public CronExpression cron() {
        return cron != null ?
            CronExpression.parse(cron) :
            null;
    }

    @Override
    public TaskLink cron(CronExpression cron) {
        this.cron = cron == null
                ? null
                : cron.toString();
        return this;
    }

    public TaskLink cron(String cron) {
        this.cron = cron.isBlank()
                ? null
                : cron;
        return this;
    }

    @Override
    public TaskLink parentId(TaskID parentId) {
        throw new IllegalArgumentException("Cannot set parent ID of TaskLink");
    }

    @Override
    public TaskID parentId() {
        return ID.of(parent);
    }

    @Override
    public TaskID childId() {
        return ID.of(child);
    }

    @Override
    public TaskNodeDTO toDTO() {
        return new TaskNodeDTO()
                .parentId(ID.of(parent))
                .childId(ID.of(child))
                .position(position)
                .positionFrozen(positionFrozen)
                .skipToChildren(skipToChildren)
                .importance(importance)
                .completed(completed)
                .recurring(recurring)
                .cycleToEnd(cycleToEnd)
                .cron(cron != null ? CronExpression.parse(cron) : null)
                .projectDurationLimit(Optional.ofNullable(projectDurationLimit))
                .projectStepCountLimit(Optional.ofNullable(projectStepCountLimit))
                .projectEtaLimit(projectEtaLimit != null
                        ? Optional.of(LocalTime.parse(projectEtaLimit))
                        : Optional.empty());
    }

    @Override
    public Duration duration() {
        Duration duration = child.duration();
        if (child.project()) {
            if (projectDurationLimit != null && duration.compareTo(projectDurationLimit) > 0) {
                duration = projectDurationLimit;
            }
            if (projectEtaLimit != null && !projectEtaLimit.isBlank()) {
                Duration difference = Duration.between(LocalTime.now(), LocalTime.parse(projectEtaLimit));
                if (difference.isNegative()) {
                    difference = Duration.ZERO;
                }
                if (duration.compareTo(difference) > 0) {
                    duration = difference;
                }
            }
            // TODO: Project step count limit somehow?
        }
        return duration;
    }

    public TaskLink projectEtaLimit(LocalTime projectEtaLimit) {
        this.projectEtaLimit = (projectEtaLimit != null)
                ? projectEtaLimit.toString()
                : null;
        return this;
    }

    @Override
    public TaskLink projectDurationLimit(Optional<Duration> projectDurationLimit) {
        this.projectDurationLimit = projectDurationLimit.orElse(null);
        return this;
    }

    @Override
    public TaskLink projectStepCountLimit(Optional<Integer> projectStepCountLimit) {
        this.projectStepCountLimit = projectStepCountLimit.orElse(null);
        return this;
    }

    @Override
    public TaskLink projectEtaLimit(Optional<LocalTime> projectEtaLimit) {
        this.projectEtaLimit(projectEtaLimit.orElse(null));
        return this;
    }

    @NonNull
    public Optional<Duration> projectDurationLimit() {
        return Optional.ofNullable(projectDurationLimit);
    }

    @NonNull
    public Optional<Integer> projectStepCountLimit() {
        return Optional.ofNullable(projectStepCountLimit);
    }

    @NonNull
    public Optional<LocalTime> projectEtaLimit() {
        return Optional.ofNullable(projectEtaLimit != null
                ? LocalTime.parse(projectEtaLimit)
                : null);
    }

    @Override
    public Boolean completed() {
        if (completed && completedAt == null) {
            completedAt = K.EPOCH_DATE;
        }
        return completed;
    }

    @Override
    public TaskLink completed(Boolean completed) {
        this.completed = completed;
        if (completed != null) {
            this.completedAt = completed ? LocalDateTime.now() : null;
        }
        return this;
    }
}