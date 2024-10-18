package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineStepData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
import com.trajan.negentropy.model.interfaces.Timeable;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "routine_steps", indexes = {
        @Index(columnList = "id", name = "idx_step_id"),
        @Index(columnList = "status", name = "idx_status"),
        @Index(columnList = "startTime, finishTime", name = "idx_time"),
        @Index(columnList = "link_id", name = "idx_step_link"),
        @Index(columnList = "task_id", name = "idx_step_task"),
        @Index(columnList = "routine_id", name = "idx_step_routine"),
        @Index(columnList = "parent_step_id", name = "idx_parent_step")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class RoutineStepEntity extends AbstractEntity implements Timeable<RoutineStepEntity>, RoutineStepData<RoutineStepEntity>,
        HasTaskLinkOrTaskEntity {

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
            fetch = FetchType.LAZY,
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

    private Boolean deletedLink = false;

    public RoutineStepEntity(HasTaskLinkOrTaskEntity entity) {
        if (entity.link().isPresent()) this.link = entity.link().get();
        this.task = entity.task();
    }

    public RoutineStepEntity(TaskLink link) {
        this.link = link;
        this.task = link.child();
    }

    public RoutineStepEntity(TaskEntity task) {
        this.task = task;
    }

    public String name() {
        return task.name();
    }

    public String description() {
        return task.name();
    }

    @Override
    public Duration duration() {
        return task.duration();
    }

    public Optional<TaskLink> link() {
        return Optional.ofNullable(link);
    }

    @Override
    public List<RoutineStepEntity> descendants() {
        return DFSUtil.traverse(this);
    }

    @Override
    public String toString() {
        return "RoutineStepEntity(" + id + ")[name=" + name() + ", link=" + link().isPresent() + ", status=" + status
                + ", position=" + position + ", startTime=" + startTime + ", finishTime=" + finishTime + "]";
    }
}