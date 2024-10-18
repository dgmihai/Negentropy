package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("routineStepRepository")
public interface RoutineStepRepository extends BaseRepository<RoutineStepEntity, Long> {

    @Query("SELECT DISTINCT s FROM RoutineStepEntity s " +
            "LEFT JOIN FETCH s.link " +
            "LEFT JOIN FETCH s.task " +
            "WHERE s.routine.id = :routineId")
    List<RoutineStepEntity> findAllByRoutineId(@Param("routineId") Long routineId);
}
