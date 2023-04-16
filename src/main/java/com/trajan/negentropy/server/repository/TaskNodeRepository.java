package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("taskNodeRepository")
@Transactional
public interface TaskNodeRepository extends JpaRepository<TaskNode, Long>, JpaSpecificationExecutor<TaskNode>, GenericSpecificationProvider<TaskNode> {
    default List<TaskNode> findByFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TaskNode> spec = getSpecificationFromFilters(filters, TaskNode.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }
}
