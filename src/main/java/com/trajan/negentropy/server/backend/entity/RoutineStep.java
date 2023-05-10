package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.backend.entity.status.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "routine_steps")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class RoutineStep extends AbstractEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    private TaskEntity task;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime suspendTime;

    private Duration elapsedSuspendedTime = Duration.ZERO;

    @ManyToOne
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TaskStatus status;

    private Integer position;

    public String toString() {
        return "Step(" + super.toString() + ", " + task + ")";
    }
}