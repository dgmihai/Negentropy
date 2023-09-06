package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.server.backend.util.DFSUtil;
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
public class RoutineEntity extends AbstractEntity implements RoutineData, Ancestor<RoutineStepEntity> {
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderColumn(name = "position")
    private List<RoutineStepEntity> children = new ArrayList<>();

    private Integer currentPosition = 0;

    private LocalDateTime estimatedDurationLastUpdatedTime;
    private Duration estimatedDuration;

    @Enumerated(EnumType.STRING)
    private TimeableStatus status = TimeableStatus.NOT_STARTED;

    public List<RoutineStepEntity> getAllChildren() {
        return DFSUtil.traverse(children.get(0));
    }

    public RoutineStepEntity currentStep() {
        return getAllChildren().get(currentPosition);
    }

    public LocalDateTime finishTime() {
        return currentStep().finishTime();
    }

    public int countSteps() {
        return getAllChildren().size();
    }
}