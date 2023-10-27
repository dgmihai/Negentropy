package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.stream.Stream;

@Repository("routineRepository")
public interface RoutineRepository extends BaseRepository<RoutineEntity, Long> {

    @Query("SELECT DISTINCT r FROM RoutineEntity r JOIN r.children c WHERE c.task IN :tasks AND r.id IN :routineIds")
    Stream<RoutineEntity> findRoutinesByTasksAndRoutineIds(
            @Param("tasks") Collection<TaskEntity> tasks,
            @Param("routineIds") Collection<Long> routineIds);
}