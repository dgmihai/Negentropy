package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import org.springframework.stereotype.Repository;

@Repository("routineRepository")
public interface RoutineRepository extends BaseRepository<RoutineEntity, Long> {
}