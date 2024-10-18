package com.trajan.negentropy.server.backend.sync;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.AbstractEntity;
import com.trajan.negentropy.model.entity.sync.ChangeRecordEntity;
import com.trajan.negentropy.model.entity.sync.QSyncRecordEntity;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.entity.sync.SyncRecordEntity;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.HasDuration;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordDataType;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordType;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.ChangeRecordRepository;
import com.trajan.negentropy.server.backend.repository.SyncRecordRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Getter
@Slf4j
@Component
@Transactional
@Benchmark(millisFloor = 10)
public class SyncManager {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private SyncRecordRepository syncRecordRepository;
    @Autowired private ChangeRecordRepository changeRecordRepository;
    @Autowired private DataContext dataContext;

    private final Map<Long, ChangeRecordType> pendingChangeTypes = new HashMap<>();
    private final Map<Long, ChangeRecordEntity> pendingChangeRecords = new HashMap<>();
    private SyncRecordEntity pendingSyncRecord = new SyncRecordEntity();
    public static SyncManager instance;

    @PostConstruct
    public void init() {
        log.debug("Initializing SyncManager");
        instance = this;

        syncRecordRepository.findAll().forEach(syncResponse -> {
            log.debug("Clearing sync from {}", syncResponse.timestamp());
            syncResponse.changes().clear();
            syncRecordRepository.deleteAll();
        });
    }

    private SyncRecordEntity getLatestSyncRecordEntity() {
        List<SyncRecordEntity> syncRecordEntities = syncRecordRepository.findAll(Sort.by(Direction.DESC, "id"));
        if (syncRecordEntities.isEmpty()) {
            SyncRecordEntity newSyncRecord = syncRecordRepository.save(new SyncRecordEntity());
            log.debug("No sync records found, creating new base record: {}, timestamp: {}", newSyncRecord.id(), newSyncRecord.timestamp());
            return newSyncRecord;
        } else {
            SyncRecordEntity currentSyncRecord = syncRecordEntities.get(0);
            log.debug("Found {} sync records, using the latest sync: id {}, timestamp {}", syncRecordEntities.size(),
                    currentSyncRecord.id(), currentSyncRecord.timestamp());
            return currentSyncRecord;
        }
    }

    public synchronized void logChange(ChangeRecordType changeType, ChangeRecordDataType dataType, AbstractEntity entity) {
        Long id = entity.id();
        log.trace("Logging change: {} {}, id: {}", dataType, changeType, id);
        ChangeRecordType finalChangeType =
                pendingChangeTypes.containsKey(id) && changeType.ordinal() < pendingChangeTypes.get(id).ordinal()
                ? pendingChangeTypes.get(id)
                : changeType;
        pendingChangeTypes.put(id, finalChangeType);

        Duration previousDuration = null;
        if (entity instanceof HasDuration durationableEntity) {
            previousDuration = durationableEntity.duration();
        }

        pendingChangeRecords.put(id, new ChangeRecordEntity(
                null,
                LocalDateTime.now(),
                finalChangeType,
                dataType,
                id,
                previousDuration));
    }

    public synchronized SyncRecord aggregatedSyncRecord(SyncID from) {
        if (!pendingChangeRecords.isEmpty()) {
            log.debug("Processing {} pending change records", pendingChangeRecords.size());
            for (ChangeRecordEntity changeRecord : pendingChangeRecords.values()) {
                changeRecord = changeRecordRepository.save(changeRecord);
                pendingSyncRecord.changes().add(changeRecord);
            }
            pendingChangeRecords.clear();
            pendingChangeTypes.clear();
        }

        if (!pendingSyncRecord.changes().isEmpty()) {
            SyncRecordEntity latestSyncRecord = syncRecordRepository.save(pendingSyncRecord);
            log.info("Recording {} changes, new latest sync id {}", latestSyncRecord.changes().size(),
                    latestSyncRecord.id());
            pendingSyncRecord = new SyncRecordEntity();
        } else {
            log.debug("No pending changes to record");
        }

        Stream<SyncRecordEntity> syncRecordStream;
        if (from != null) {
            log.trace("Getting sync records from {}", from);
            syncRecordStream = StreamSupport.stream(syncRecordRepository.findAll(new BooleanBuilder(
                    QSyncRecordEntity.syncRecordEntity.id.gt(from.val()))).spliterator(), true);
            return toDO(syncRecordStream);
        } else {
            log.warn("No sync id provided, returning latest sync record");
            return toDO(Stream.of(getLatestSyncRecordEntity()));
        }
    }

    public SyncID getCurrentSyncId() {
        return getLatestSyncRecordEntity().id();
    }

    private SyncRecord toDO(Stream<SyncRecordEntity> syncRecordStream) {
        List<ChangeRecordEntity> changeRecordEntities = syncRecordStream
                .flatMap(syncRecordEntity -> syncRecordEntity.changes().stream())
                .peek(record -> log.trace("Recorded change: {} {} {}", record.changeType(), record.dataType(), record.entityId()))
                .toList();

        Map<Long, Map<ChangeRecordDataType, Set<ChangeRecordType>>> entityChangeTypeMap = changeRecordEntities.stream()
                .collect(Collectors.groupingBy(
                        ChangeRecordEntity::entityId,
                        Collectors.groupingBy(ChangeRecordEntity::dataType,
                                Collectors.mapping(ChangeRecordEntity::changeType, Collectors.toSet())
                        )
                ));

        List<Change> changes = entityChangeTypeMap.entrySet().stream()
                .flatMap(entry -> {
                    Long entityId = entry.getKey();
                    Map<ChangeRecordDataType, Set<ChangeRecordType>> dataTypesMap = entry.getValue();

                    return dataTypesMap.entrySet().stream().map(dataTypeEntry -> {
                        ChangeRecordDataType dataType = dataTypeEntry.getKey();
                        Set<ChangeRecordType> changeTypes = dataTypeEntry.getValue();

                        if (changeTypes.contains(ChangeRecordType.DELETE)) {
                            return getDeleteChange(dataType, entityId);
                        } else if (changeTypes.contains(ChangeRecordType.PERSIST)) {
                            return new PersistChange<>(convertData(dataType, entityId));
                        } else {
                            return new MergeChange<>(convertData(dataType, entityId));
                        }
                    });
                })
                .distinct()
                .collect(Collectors.toList());

        SyncRecordEntity latestSyncRecord = this.getLatestSyncRecordEntity();

        return new SyncRecord(
                latestSyncRecord.id(),
                latestSyncRecord.timestamp(),
                changes);
    }

    private Change getDeleteChange(ChangeRecordDataType dataType, Long id) {
        return switch (dataType) {
            case TASK -> new DeleteChange<>(new TaskID(id));
            case LINK -> new DeleteChange<>(new LinkID(id));
            case TAG -> new DeleteChange<>(new TagID(id));
        };
    }

    private PersistedDataDO<?> convertData(ChangeRecordDataType dataType, Long id) {
        return switch (dataType) {
            case TASK -> dataContext.toEagerDO(entityQueryService.getTask(new TaskID(id)));
            case LINK -> dataContext.toEagerDO(entityQueryService.getLink(new LinkID(id)));
            case TAG -> dataContext.toDO(entityQueryService.getTag(new TagID(id)));
        };
    }
}
