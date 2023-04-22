package com.trajan.negentropy.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Entity
@Table(name = "task_duration_estimates")
@AllArgsConstructor
@NoArgsConstructor
public class DurationEstimate extends AbstractEntity {
    @ManyToOne
    private Task task;

    private int priority;

    private Duration additionalDuration;
}
