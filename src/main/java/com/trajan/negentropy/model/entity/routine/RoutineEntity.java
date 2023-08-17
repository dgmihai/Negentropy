package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TimeableStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString(callSuper = true)
public class RoutineEntity extends AbstractEntity implements RoutineData {
    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "routine",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderColumn(name = "position")
    private List<RoutineStepEntity> steps = new ArrayList<>();

    private Integer currentPosition = 0;

    private LocalDateTime estimatedDurationLastUpdatedTime;
    private Duration estimatedDuration;

    @Enumerated(EnumType.STRING)
    private TimeableStatus status = TimeableStatus.NOT_STARTED;

    public RoutineStepEntity currentStep() {
        return steps.get(currentPosition);
    }

    public LocalDateTime finishTime() {
        return currentStep().finishTime();
    }
}