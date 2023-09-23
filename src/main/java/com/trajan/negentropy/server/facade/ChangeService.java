package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.server.Broadcaster;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.sync.SyncManager;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
@Slf4j
@Benchmark
public class ChangeService {
    @Autowired private ChangeProcessor processor;
    @Autowired private SyncManager syncManager;
    @Autowired private QueryService queryService;
    @Autowired private EntityQueryService entityQueryService;

    @Autowired private Broadcaster broadcaster;

    public synchronized DataMapResponse execute(Request request) {
        log.debug("Processing change request with sync id {}, change count {}",
                request.syncId(),
                request.changes().size());
        boolean sync = request.syncId() != null;
        try {
            Pair<String, MultiValueMap<Integer, PersistedDataDO<?>>> processResults = processor.process(request);
            String resultMessage = processResults.getFirst();
            MultiValueMap<Integer, PersistedDataDO<?>> dataResults = processResults.getSecond();

            log.trace("Data results: " + dataResults);

            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : null;

            broadcaster.broadcast(aggregateSyncRecord);

            return new DataMapResponse(true, resultMessage, dataResults, aggregateSyncRecord);
        } catch (Exception e) {
            e.printStackTrace();
            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : null;

            return new DataMapResponse(false, e.getMessage(), aggregateSyncRecord);
        }
    }
}