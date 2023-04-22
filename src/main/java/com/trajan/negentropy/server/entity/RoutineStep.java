package com.trajan.negentropy.server.entity;

import com.trajan.negentropy.server.entity.status.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "steps")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RoutineStep extends AbstractEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    private Task task;

    private LocalDateTime start;
    private LocalDateTime finish;

    @Builder.Default
    private Duration elapsedActiveTime = Duration.ZERO;

    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    @JoinColumn(name = "routine_id")
    private Routine routine;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TaskStatus status;

    public String toString() {
        return "Step(" + super.toString() + ", " + task + ")";
    }
}