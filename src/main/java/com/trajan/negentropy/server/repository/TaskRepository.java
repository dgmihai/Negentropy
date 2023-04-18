package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("taskInfoRepository")
@Transactional
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task>, GenericSpecificationProvider<Task> {
    default List<Task> findAllFiltered(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<Task> spec = getSpecificationFromFilters(filters, Task.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }
}
