package com.trajan.negentropy.server.backend.entity;

import com.trajan.negentropy.server.facade.model.interfaces.RoutineStepData;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private TaskLink link;

    @ManyToOne
    @JoinColumn(name = "parent_step_id")
    private RoutineStepEntity parent;

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parent",
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("position")
    private List<RoutineStepEntity> children = new ArrayList<>();;

    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime lastSuspendedTime;

    private Duration elapsedSuspendedDuration = Duration.ZERO;

    @ManyToOne
    @JoinColumn(name = "routine_id")
    @ToString.Exclude
    private RoutineEntity routine;

    @Enumerated(EnumType.STRING)
    @NotNull
    private TimeableStatus status = TimeableStatus.NOT_STARTED;

    private Integer position;

    @Override
    public Duration duration() {
        return link.child().duration();
    }
}