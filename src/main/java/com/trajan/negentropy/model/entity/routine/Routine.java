package com.trajan.negentropy.model.entity.routine;

import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class Routine implements RoutineData<RoutineStep> {
    private RoutineID id;

    private Map<StepID, RoutineStep> steps;

    @ToString.Exclude
    private List<StepID> childrenIds;

    private int currentPosition;

    private TimeableStatus status;

    private Boolean autoSync;
    private SyncID syncId;

    public RoutineStep currentStep() {
        return this.getAllChildren().get(currentPosition);
    }

    public RoutineStep rootStep() {
        return steps.get(childrenIds.get(0));
    }

    @ToString.Include
    public String name() {
        return rootStep().name();
    }

    public Duration estimatedDuration() {
        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);
        return rootStep().status().equals(TimeableStatus.NOT_STARTED)
                ? getAllChildren().stream().map(RoutineStep::duration).reduce(Duration.ZERO, Duration::plus)
                : timeableUtil.getRemainingNetDuration(rootStep(), rootStep().startTime);
    }

    @Override
    public LocalDateTime startTime() {
        return rootStep().startTime();
    }

    @Override
    public List<RoutineStep> getAllChildren() {
        return DFSUtil.traverse(rootStep());
    }

    @Override
    public LocalDateTime finishTime() {
        return currentStep().finishTime();
    }

    public int countSteps() {
        return DFSUtil.traverse(rootStep()).size();
    }

    public List<RoutineStep> children() {
        return childrenIds.stream().map(steps::get).toList();
    }
}