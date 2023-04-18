package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskSession;
import com.trajan.negentropy.server.entity.TaskSession_;
import com.trajan.negentropy.server.entity.TaskStatus;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskSessionRepository extends JpaRepository<TaskSession, Long>, JpaSpecificationExecutor<TaskSession>, GenericSpecificationProvider<TaskSession> {
    Optional<TaskSession> findByNodeId(long taskId);

    default TaskSession findActiveTaskSession() {
        Filter statusFilter = Filter.builder()
                .field(TaskSession_.STATUS)
                .operator(QueryOperator.EQUALS)
                .value(TaskStatus.ACTIVE)
                .build();
        List<Filter> filters = Collections.singletonList(statusFilter);
        Specification<TaskSession> specification = getSpecificationFromFilters(filters, TaskSession.class);
        return findOne(specification).orElse(null);
    }

    default List<TaskSession> findAllFiltered(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll();
        } else {
            Specification<TaskSession> spec = getSpecificationFromFilters(filters, TaskSession.class);
            return new ArrayList<>(findAll(spec, Sort.unsorted()));
        }
    }
}