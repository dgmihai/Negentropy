package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines", indexes = {
        @Index(columnList = "autoSync", name = "idx_routine_auto_sync"),
        @Index(columnList = "status", name = "idx_routine_status")
})
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class RoutineEntity extends AbstractEntity implements RoutineData, Ancestor<RoutineStepEntity> {
    
    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "routine_seq", sequenceName = "routine_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "routine_seq")
    private Long id;
    
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderColumn(name = "position")
    private List<RoutineStepEntity> children = new ArrayList<>();

    private Integer currentPosition = 0;

    @Enumerated(EnumType.STRING)
    private TimeableStatus status = TimeableStatus.NOT_STARTED;

    private Duration customProjectDuration;

    @Column(columnDefinition="TEXT")
    private String serializedFilter;

    private Boolean autoSync= true;
    private Long syncId;

    public SyncID syncId() {
        return (syncId != null) ? new SyncID(syncId) : null;
    }

    @Override
    public RoutineStepEntity currentStep() {
        return (RoutineStepEntity) RoutineData.super.currentStep();
    }

    @Override
    public List<RoutineStepEntity> getDescendants() {
        List<RoutineStepEntity> descendants = new ArrayList<>();
        for (RoutineStepEntity child : children) {
            descendants.addAll(DFSUtil.traverse(child));
        }
        return descendants;
    }
}