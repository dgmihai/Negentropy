package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.RoutineEntity;
import org.springframework.stereotype.Repository;

@Repository("routineRepository")
public interface RoutineRepository extends BaseRepository<RoutineEntity, Long> {
}