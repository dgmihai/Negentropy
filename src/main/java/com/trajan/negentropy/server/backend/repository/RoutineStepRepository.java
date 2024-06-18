package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import org.springframework.stereotype.Repository;

@Repository("routineStepRepository")
public interface RoutineStepRepository extends BaseRepository<RoutineStepEntity, Long> { }
