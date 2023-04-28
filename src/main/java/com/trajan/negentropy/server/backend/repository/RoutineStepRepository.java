package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.RoutineStep;
import com.trajan.negentropy.server.backend.entity.status.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("taskRecordRepository")
public interface RoutineStepRepository extends JpaRepository<RoutineStep, Long>, JpaSpecificationExecutor<RoutineStep> {
    boolean existsByTaskId(Long taskId);

    List<RoutineStep> findByStatus(TaskStatus status);
}
