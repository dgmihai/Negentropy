package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.backend.entity.status.StepStatus;
import com.trajan.negentropy.server.facade.model.interfaces.RoutineStepData;
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
@ToString(callSuper = true)
public class RoutineStepEntity extends AbstractEntity implements RoutineStepData {
    @ManyToOne(fetch = FetchType.EAGER)
    @NotNull
    private TaskEntity task;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime lastSuspendedTime;

    private Duration elapsedSuspendedDuration = Duration.ZERO;

    @ManyToOne
    @JoinColumn(name = "routine_id")
    @ToString.Exclude
    private RoutineEntity routine;

    @ManyToOne
    @JoinColumn(name = "parent_step_id")
    private RoutineStepEntity parent;

    @Enumerated(EnumType.STRING)
    @NotNull
    private StepStatus status = StepStatus.NOT_STARTED;

    private Integer position;
}