package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.Routine;
import com.trajan.negentropy.server.entity.status.RoutineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("routineRepository")
public interface RoutineRepository extends JpaRepository<Routine, Long>, JpaSpecificationExecutor<Routine> {
    List<Routine> findByStatusAndRootTaskId(RoutineStatus status, long rootTaskId);

    List<Routine> findByStatus(RoutineStatus status);
}