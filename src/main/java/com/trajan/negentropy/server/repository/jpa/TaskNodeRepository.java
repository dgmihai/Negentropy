package com.trajan.negentropy.server.repository.jpa;

import com.trajan.negentropy.server.entity.TaskNode;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("taskNodeRepository")
@Transactional
public interface TaskNodeRepository extends JpaRepository<TaskNode, Long>, JpaSpecificationExecutor<TaskNode> {
}
