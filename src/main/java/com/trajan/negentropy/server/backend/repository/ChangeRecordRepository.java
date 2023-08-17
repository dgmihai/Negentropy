package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.sync.ChangeRecordEntity;
import org.springframework.stereotype.Repository;

@Repository("changeRecordRepository")
public interface ChangeRecordRepository extends BaseRepository<ChangeRecordEntity, Long> { }

