package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("taskInfoRepository")
@Transactional
public interface TaskInfoRepository extends JpaRepository<TaskInfo, Long>, JpaSpecificationExecutor<TaskInfo> {
    @Query("SELECT ti FROM TaskInfo ti JOIN FETCH ti.relationships WHERE ti.id = :id")
    Optional<TaskInfo> findByIdWithRelationships(Long id);
}
