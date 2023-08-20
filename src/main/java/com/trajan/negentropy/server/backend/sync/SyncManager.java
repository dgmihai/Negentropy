package com.trajan.negentropy.server.backend.sync;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.sync.ChangeRecordEntity;
import com.trajan.negentropy.model.entity.sync.QSyncRecordEntity;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.entity.sync.SyncRecordEntity;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
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
public class SyncManager {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private SyncRecordRepository syncRecordRepository;
    @Autowired private ChangeRecordRepository changeRecordRepository;
    @Autowired private DataContext dataContext;

    private Map<Long, ChangeRecordType> pendingChangeTypes = new HashMap<>();
    private Map<Long, ChangeRecordEntity> pendingChangeRecords = new HashMap<>();
    private Map<TaskID, Duration> netDurationRecords = new HashMap<>();
    private SyncRecordEntity pendingSyncRecord = new SyncRecordEntity();

    public static SyncManager instance;

    @PostConstruct
    public void init() {
        log.debug("Initializing SyncManager");
        instance = this;

        syncRecordRepository.findAll().forEach(syncResponse -> {
            syncResponse.changes().clear();
            syncRecordRepository.deleteAll();
        });
    }

    private SyncRecordEntity getLatestSyncRecord() {
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

    public synchronized void logChange(ChangeRecordType changeType, ChangeRecordDataType dataType, Long id) {
        log.trace("Logging change: {} {}, id: {}", dataType, changeType, id);
        ChangeRecordType finalChangeType =
                pendingChangeTypes.containsKey(id) && changeType.ordinal() < pendingChangeTypes.get(id).ordinal()
                ? pendingChangeTypes.get(id)
                : changeType;
        pendingChangeTypes.put(id, finalChangeType);
        pendingChangeRecords.put(id, new ChangeRecordEntity(
                null,
                LocalDateTime.now(),
                finalChangeType,
                dataType,
                id));
    }

    public synchronized void logDurationChange(TaskID taskId, Duration netDuration) {
        netDurationRecords.put(taskId, netDuration);
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

        if (pendingSyncRecord.changes().size() > 0) {
            SyncRecordEntity latestSyncRecord = syncRecordRepository.save(pendingSyncRecord);
            log.info("Recording {} changes, new latest sync id {}", latestSyncRecord.changes().size(),
                    latestSyncRecord.id());
            pendingSyncRecord = new SyncRecordEntity();
        } else {
            log.debug("No changes to record");
        }

        Stream<SyncRecordEntity> syncRecordStream;
        if (from != null) {
            log.trace("Getting sync records from {}", from);
            syncRecordStream = StreamSupport.stream(syncRecordRepository.findAll(new BooleanBuilder(
                    QSyncRecordEntity.syncRecordEntity.id.gt(from.val()))).spliterator(), true);
            return toDO(syncRecordStream);
        } else {
            log.warn("No sync id provided, returning empty sync record");
            return new SyncRecord(
                    getLatestSyncRecord().id(),
                    getLatestSyncRecord().timestamp(),
                    List.of(),
                    new HashMap<>());
        }
    }

    private SyncRecord toDO(Stream<SyncRecordEntity> syncRecordStream) {
        List<ChangeRecordEntity> changeRecordEntities = syncRecordStream
                .flatMap(syncRecordEntity -> syncRecordEntity.changes().stream())
                .peek(record -> log.debug("Recorded change: {} {} {}", record.changeType(), record.dataType(), record.entityId()))
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
                            return Change.persist(convertData(dataType, entityId));
                        } else {
                            return Change.merge(convertData(dataType, entityId));
                        }
                    });
                })
                .distinct()
                .collect(Collectors.toList());

        SyncRecordEntity latestSyncRecord = this.getLatestSyncRecord();

        return new SyncRecord(
                latestSyncRecord.id(),
                latestSyncRecord.timestamp(),
                changes,
                netDurationRecords);
    }

    private Change getDeleteChange(ChangeRecordDataType dataType, Long id) {
        return switch (dataType) {
            case TASK -> Change.delete(new TaskID(id));
            case LINK -> Change.delete(new LinkID(id));
            case TAG -> Change.delete(new TagID(id));
        };
    }

    private PersistedDataDO<?> convertData(ChangeRecordDataType dataType, Long id) {
        return switch (dataType) {
            case TASK -> dataContext.toDO(entityQueryService.getTask(new TaskID(id)));
            case LINK -> dataContext.toDO(entityQueryService.getLink(new LinkID(id)));
            case TAG -> dataContext.toDO(entityQueryService.getTag(new TagID(id)));
        };
    }
}
