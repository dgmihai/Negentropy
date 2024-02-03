package com.trajan.negentropy.model.entity.routine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.LinkedListMultimap;
import com.trajan.negentropy.model.data.RoutineData;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.RoutineID;
import com.trajan.negentropy.model.id.StepID;
import com.trajan.negentropy.util.SpringContext;
import com.trajan.negentropy.util.TimeableUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Getter(onMethod = @__(@JsonProperty))
@Setter
@ToString
public class Routine implements RoutineData<RoutineStep> {
    private RoutineID id;

    @ToString.Exclude
    private Map<StepID, RoutineStep> steps;
    @ToString.Exclude
    private LinkedListMultimap<StepID, StepID> childAdjacencyMap;
    @ToString.Exclude
    private List<RoutineStep> children;

    private Integer currentPosition;

    private TimeableStatus status;

    private Boolean autoSync;
    private SyncID syncId;

    @ToString.Include
    public String name() {
        String name = children.get(0).name();
        if (children.size() > 1) {
            name += " (+ " + (children.size() - 1) + " more)";
        }
        return name;
    }

    public Duration estimatedDuration() {
        TimeableUtil timeableUtil = SpringContext.getBean(TimeableUtil.class);
        Duration remainingDuration = Duration.ZERO;
        for (RoutineStep child : children) {
            remainingDuration = remainingDuration.plus(timeableUtil.getRemainingNestedDuration(child, child.startTime));
        }
        return remainingDuration;
    }

    @Override
    public List<RoutineStep> descendants() {
        return childAdjacencyMap.values().stream()
                .map(steps::get)
                .toList();
    }
}