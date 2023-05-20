package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.RoutineStepEntity;
import org.springframework.stereotype.Repository;

@Repository("taskRecordRepository")
public interface RoutineStepRepository extends BaseRepository<RoutineStepEntity, Long> {
}
