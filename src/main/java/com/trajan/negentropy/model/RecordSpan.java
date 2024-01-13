package com.trajan.negentropy.model;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.id.LinkID;
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

    @Getter
    @Setter
    public static class RecordSpanEntry extends RecordSpan {
        private LinkID linkId;

        public RecordSpanEntry(LinkID linkId, RecordSpan recordSpan) {
            super(recordSpan.task, recordSpan.resultMap, recordSpan.totalCount, recordSpan.averageDuration, recordSpan.netDuration);
            this.linkId = linkId;
        }
    }
}
