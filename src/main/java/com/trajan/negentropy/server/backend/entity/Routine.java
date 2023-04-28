package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Builder(toBuilder = true)
@Accessors(fluent = true)
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
    private List<RoutineStep> steps = new ArrayList<>();

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private TaskEntity rootTask;

    @Builder.Default
    private int currentIndex = 0;

    private Duration estimatedDuration = Duration.ZERO;

    @Enumerated(EnumType.STRING)
    private RoutineStatus status;

    private Integer priority = 0;
}