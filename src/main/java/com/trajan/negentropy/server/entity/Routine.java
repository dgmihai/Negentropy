package com.trajan.negentropy.server.entity;

import com.trajan.negentropy.server.entity.status.RoutineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Routine extends AbstractEntity {
    @OneToMany(
            mappedBy = "routine",
            cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE,
            CascadeType.REFRESH,
            CascadeType.DETACH},
            fetch = FetchType.EAGER)
    @OrderColumn(name = "step_order")
    @Builder.Default
    private List<RoutineStep> steps = new ArrayList<>();

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private Task rootTask;

    @ManyToMany(fetch = FetchType.EAGER)
    @OrderColumn(name = "queue_order")
    @Builder.Default
    private List<Task> queue = new ArrayList<>();

    @Builder.Default
    private Duration estimatedDuration = Duration.ZERO;

    @Enumerated(EnumType.STRING)
    @NotNull
    private RoutineStatus status;

    @Builder.Default
    private Integer priority = 0;

    public Routine(Task task) {
        this.rootTask = task;
    }
}