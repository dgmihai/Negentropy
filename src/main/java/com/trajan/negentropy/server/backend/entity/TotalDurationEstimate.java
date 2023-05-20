package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;

@Entity
@Table(name = "task_duration_estimates")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@IdClass(TotalDurationEstimateID.class)
public class TotalDurationEstimate {
    @Id
    @ManyToOne(
            cascade = {CascadeType.PERSIST,
                    CascadeType.MERGE}
    )
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    @Id
    private int importance = 0;

    private Duration totalDuration = Duration.ZERO;
}