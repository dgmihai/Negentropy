package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordDataType;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordType;
import com.trajan.negentropy.server.backend.sync.SyncManager;
import com.trajan.negentropy.server.broadcaster.ServerBroadcaster;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.util.ServerClockService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@Benchmark
public class ChangeService {
    @Autowired private ServerClockService clock;
    @Autowired private ChangeProcessor processor;
    @Autowired private SyncManager syncManager;

    @Autowired private ServerBroadcaster broadcaster;

    public static int DAYS_UNTIL_CLEANUP = 7;

    @PostConstruct
    public void init() {
        log.info("Initializing ChangeService");
        processor.updateSyncManagerDurations(durationUpdates -> {
            durationUpdates.forEach(link ->
                    syncManager.logChange(ChangeRecordType.MERGE, ChangeRecordDataType.LINK, link));
        });

        this.execute(processor.entityQueryService().findLinks(new TaskNodeTreeFilter()
                        .completed(true)
                        .recurring(false)
                        .hasChildren(false)
                        .completedBefore(clock.time()
                                .minusDays(DAYS_UNTIL_CLEANUP)))
                .map(link -> new DeleteChange<>(ID.of(link)))
                .toArray(Change[]::new));
    }

    public synchronized DataMapResponse execute(Change... changes) {
        return execute(Request.of(changes));
    }

    public synchronized DataMapResponse execute(Request request) {
        log.debug("Processing change request with sync id {}, change count {}",
                request.syncId(),
                request.changes().size());
        request.changes().forEach(change -> log.debug("> " + change.toString()));
        boolean sync = request.syncId() != null;
        try {
            Pair<String, MultiValueMap<ChangeID, PersistedDataDO<?>>> processResults = processor.process(request);
            String resultMessage = processResults.getFirst();
            MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults = processResults.getSecond();

            log.trace("Data results: " + dataResults);

            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : syncManager.aggregatedSyncRecord(null);

            broadcaster.broadcast(aggregateSyncRecord);

            return new DataMapResponse(true, resultMessage, dataResults, aggregateSyncRecord);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : null;

            return new DataMapResponse(false, e.getMessage(), aggregateSyncRecord);
        }
    }
}