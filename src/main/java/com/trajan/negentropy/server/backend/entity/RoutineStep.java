package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.backend.entity.status.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "steps")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(fluent = true)
@Getter
@Setter
public class RoutineStep extends AbstractEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    private TaskEntity task;

    private LocalDateTime start;
    private LocalDateTime finish;
    private LocalDateTime lastResumed;
    @Builder.Default
    private Duration elapsedActiveTime = Duration.ZERO;

    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH})
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TaskStatus status;

    public String toString() {
        return "Step(" + super.toString() + ", " + task + ")";
    }
}