package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class Routine extends AbstractEntity {
    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "routine",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderColumn(name = "position")
    private List<RoutineStep> steps = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    private TaskEntity rootTask;

    @OneToOne(fetch = FetchType.EAGER)
    private RoutineStep current;

    private Duration estimatedDuration = Duration.ZERO;

    @Enumerated(EnumType.STRING)
    private RoutineStatus status;

    private Integer priority = 0;
}