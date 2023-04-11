package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskRelationship;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("taskRelationshipRepository")
@Transactional
public interface TaskRelationshipRepository extends JpaRepository<TaskRelationship, Long>, JpaSpecificationExecutor<TaskRelationship> {
}
