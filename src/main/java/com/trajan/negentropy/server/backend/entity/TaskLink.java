package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.facade.model.interfaces.Descendant;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_links")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@Slf4j
public class TaskLink extends AbstractEntity implements Descendant<TaskEntity> {
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TaskEntity parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    @NotNull
    private TaskEntity child;

    private Integer position = 0;
    private Integer importance = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private Boolean completed = false;

    private Boolean recurring = false;
    private String cron;
    private LocalDateTime scheduledFor = null;

    private Duration projectDuration = null;

    public String toString() {
        return "LinkEntity[" + super.toString() + ", parent=" + parent + ", child=" + child + "]";
    }

    public CronExpression cron() {
        return cron != null ?
            CronExpression.parse(cron) :
            null;
    }

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
}