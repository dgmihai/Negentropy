package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;

@Entity
@Table(name = "task_duration_estimates")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TimeEstimate extends AbstractEntity {
    @ManyToOne
    @NotNull
    private TaskEntity task;

    private int priority = 0;

    private Duration duration;
}