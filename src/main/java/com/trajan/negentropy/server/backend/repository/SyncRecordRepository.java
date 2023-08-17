package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.sync.SyncRecordEntity;
import org.springframework.stereotype.Repository;

@Repository("syncRecordRepository")
public interface SyncRecordRepository extends BaseRepository<SyncRecordEntity, Long> { }

