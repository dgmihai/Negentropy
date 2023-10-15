package com.trajan.negentropy.model.entity;

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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@EntityListeners(SyncManagerListener.class)
@Table(name = "task_links")
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
    private Integer importance = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean completed = false;

    private Boolean recurring = false;
    private String cron;
    private LocalDateTime scheduledFor = null;

    private Duration projectDuration = Duration.ZERO;

    public String toString() {
        return "LinkEntity[" + super.toString() + ", parent=" + parent + ", position=" + position + ", child=" + child + "]";
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
        throw new NotImplementedException("TaskLink.toDTO() not implemented");
    }

    @Override
    public Duration duration() {
        return (child.project() && this.projectDuration != null)
                ? projectDuration
                : child.duration();
    }
}