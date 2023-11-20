package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("taskRecordRepository")
public interface RoutineStepRepository extends BaseRepository<RoutineStepEntity, Long> {
    List<RoutineStepEntity> getStepByRoutineId(Long routineId);
}
