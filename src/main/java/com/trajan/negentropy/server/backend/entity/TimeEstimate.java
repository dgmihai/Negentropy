package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;

@Entity
@Table(name = "TASK_TIME_ESTIMATES")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TimeEstimate extends AbstractEntity {
    @ManyToOne(
            cascade = {CascadeType.PERSIST,
                       CascadeType.MERGE}
    )
    @NotNull
    private TaskEntity task;

    @NotNull
    private int importance = 0; // Currently unused

    private Duration netDuration = Duration.ZERO;
}