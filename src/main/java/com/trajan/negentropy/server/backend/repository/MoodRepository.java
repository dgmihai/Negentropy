package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.MoodEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface MoodRepository extends BaseRepository<MoodEntity, Long> {
    public MoodEntity findTopByOrderByTimestampDesc();
}
