package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.client.K;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;

@RequiredArgsConstructor
@Getter
@Slf4j
public class Response {
    protected final boolean success;
    protected final String message;

    @Getter
    public static class SyncResponse extends Response {
        protected final SyncRecord aggregateSyncRecord;

        public SyncResponse(String message, SyncRecord aggregateSyncRecord) {
            super(false, message);
            this.aggregateSyncRecord = aggregateSyncRecord;
        }

        public SyncResponse(SyncRecord aggregateSyncRecord) {
            super(true, K.OK);
            this.aggregateSyncRecord = aggregateSyncRecord;
        }

        public SyncResponse(String message) {
            super(false, message);
            this.aggregateSyncRecord = null;
        }
    }

    @Getter
    public static class DataResponse<T> extends SyncResponse {
        protected T changeRelevantDataMap = null;

        public DataResponse(String message, SyncRecord aggregateSyncRecord) {
            super(message, aggregateSyncRecord);
        }

        public DataResponse(T changeRelevantDataMap, SyncRecord aggregateSyncRecord) {
            super(aggregateSyncRecord);
            this.changeRelevantDataMap = changeRelevantDataMap;
        }

        public DataResponse(String message) {
            super(message);
        }
    }

    @Getter
    public static class DataMapResponse extends DataResponse<MultiValueMap<Integer, PersistedDataDO<?>>> {
        public DataMapResponse(String message, SyncRecord aggregateSyncRecord) {
            super(message, aggregateSyncRecord);
        }

        public DataMapResponse(MultiValueMap<Integer, PersistedDataDO<?>> resultsMap, SyncRecord aggregateSyncRecord) {
            super(resultsMap, aggregateSyncRecord);
        }

        public DataMapResponse(String message) {
            super(message);
        }
    }

    public static Response ok() {
        return new Response(true, K.OK);
    }
}
