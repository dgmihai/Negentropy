package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.Routine;
import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("routineRepository")
public interface RoutineRepository extends JpaRepository<Routine, Long>, JpaSpecificationExecutor<Routine> {
    List<Routine> findByStatusAndRootTaskId(RoutineStatus status, long rootTaskId);

    List<Routine> findByStatus(RoutineStatus status);
}