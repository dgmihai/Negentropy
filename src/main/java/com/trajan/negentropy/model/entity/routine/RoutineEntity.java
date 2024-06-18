package com.trajan.negentropy.model.entity.routine;

import com.google.common.collect.ListMultimap;
import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.StepID;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import com.trajan.negentropy.util.TimeableUtil.TimeableAncestor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routines", indexes = {
        @Index(columnList = "id", name = "idx_routine_id"),
        @Index(columnList = "autoSync", name = "idx_routine_auto_sync"),
        @Index(columnList = "status", name = "idx_routine_status")
})
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class RoutineEntity extends AbstractEntity implements RoutineData<RoutineStepEntity>, Ancestor<RoutineStepEntity>,
        TimeableAncestor<RoutineStepEntity> {
    
    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "routine_seq", sequenceName = "routine_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "routine_seq")
    private Long id;
    
    @OneToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    @OrderColumn(name = "position")
    private List<RoutineStepEntity> children = new ArrayList<>();

    private Integer currentPosition = 0;

    @Enumerated(EnumType.STRING)
    private TimeableStatus status = TimeableStatus.NOT_STARTED;

    private Duration customProjectDuration;

    @Column(columnDefinition="TEXT")
    private String serializedFilter;

    private Boolean autoSync = true;
    private Long syncId;

    private LocalDateTime creationTimestamp;

    private Boolean cleaned = false;

    public SyncID syncId() {
        return (syncId != null) ? new SyncID(syncId) : null;
    }

    @Override
    public RoutineStepEntity currentStep() {
        if (currentPosition < 0) currentPosition = 0;
        return RoutineData.super.currentStep();
    }

    public boolean currentStep(RoutineStepEntity step) {
        int position = descendants().indexOf(step);
        if (position < 0) return false;
        this.currentPosition(position);
        return true;
    }

    public void currentPosition(int position) {
        log.debug("Updating current position to: " + position);
        if (position < 0) {
            String msg = "Provided position is negative";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        this.currentPosition = position;
    }

    private List<RoutineStepEntity> traverse(StepID rootId, ListMultimap<StepID, RoutineStepEntity> multimap) {
        List<RoutineStepEntity> nodes = new ArrayList<>();
        recurse(rootId, nodes, multimap);
        return nodes;
    }

    private void recurse(StepID currentId, List<RoutineStepEntity> nodes, ListMultimap<StepID, RoutineStepEntity> multimap) {
        List<RoutineStepEntity> children = multimap.get(currentId);

        for (RoutineStepEntity child : children) {
            nodes.add(child);
            recurse(ID.of(child), nodes, multimap);
        }
    }

    public List<RoutineStepEntity> descendants(EntityQueryService entityQueryService) {
        ListMultimap<StepID, RoutineStepEntity> multimap = entityQueryService
                .findRoutineStepsDescendantMapByRoutineId(ID.of(this));
        List<RoutineStepEntity> descendants = new ArrayList<>();

        for (RoutineStepEntity child : children()) {
            descendants.add(child);
            descendants.addAll(this.traverse(ID.of(child), multimap));
        }
        return descendants;
    }

    @Override
    public List<RoutineStepEntity> descendants() {
        List<RoutineStepEntity> descendants = new ArrayList<>();
        for (RoutineStepEntity child : children) {
            descendants.addAll(DFSUtil.traverse(child));
        }
        return descendants;
//        return descendants(SpringContext.getBean(EntityQueryService.class));
    }

    @Override
    public String toString() {
        return "RoutineEntity(" + id + ")[roots=" + children().stream().map(RoutineStepEntity::name).toList()
                + ", status=" + status + ", currentPosition=" + currentPosition + "]";
    }
}