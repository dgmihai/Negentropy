package com.trajan.negentropy.server.repository.jpa;

import com.trajan.negentropy.server.entity.TaskInfo;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("taskInfoRepository")
@Transactional
public interface TaskInfoRepository extends JpaRepository<TaskInfo, Long>, JpaSpecificationExecutor<TaskInfo> {
//    @Query("SELECT ti FROM TaskInfo ti JOIN FETCH ti.relationships WHERE ti.id = :id")
//    Optional<TaskInfo> findByIdWithRelationships(Long id);
}
