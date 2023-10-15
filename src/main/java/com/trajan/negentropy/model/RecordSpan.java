package com.trajan.negentropy.model;

import com.trajan.negentropy.model.entity.TimeableStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class RecordSpan {
    private Task task;
    private Map<TimeableStatus, List<Record>> resultMap;
    private int totalCount;
    private Duration averageDuration;
    private Duration netDuration;

}
