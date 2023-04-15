package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import com.trajan.negentropy.server.repository.jpa.TaskNodeRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Repository
@Transactional
public class FilteredTaskNodeRepository extends GenericSpecificationProvider<TaskNode> {
    @Autowired
    private TaskNodeRepository taskNodeRepository;

    public List<TaskNode> findByFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TaskNode> spec = getSpecificationFromFilters(filters, TaskNode.class);
            return taskNodeRepository.findAll(spec);
        } else {
            return taskNodeRepository.findAll();
        }
    }
}