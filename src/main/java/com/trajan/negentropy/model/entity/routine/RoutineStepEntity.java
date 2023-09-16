package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.Ancestor;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "routine_steps")
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString(callSuper = true)
@Slf4j
public class RoutineStepEntity extends AbstractEntity implements RoutineStepData, Ancestor<RoutineStepEntity> {
    
    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "routine_step_seq", sequenceName = "routine_step_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "routine_step_seq")
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @Getter(AccessLevel.NONE)
    private TaskLink link;

    @ManyToOne(fetch = FetchType.EAGER)
    private TaskEntity task;

    @ManyToOne
    @JoinColumn(name = "parent_step_id")
    @ToString.Exclude
    private RoutineStepEntity parentStep;

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parentStep",
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("position")
    @ToString.Exclude
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

    public RoutineStepEntity(TaskLink link) {
        this.link = link;
        this.task = link.child();
    }

    public RoutineStepEntity(TaskEntity task) {
        this.task = task;
    }

    @Override
    public Duration duration() {
        return task.duration();
    }

    public Optional<TaskLink> link() {
        return Optional.ofNullable(link);
    }
}