package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskRelationship;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Repository
@Transactional
public class FilteredTaskRelationshipRepository implements GenericSpecificationProvider<TaskRelationship> {
    @Autowired
    private TaskRelationshipRepository taskRelationshipRepository;

    public List<TaskRelationship> findByFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TaskRelationship> spec = getSpecificationFromFilters(filters, TaskRelationship.class);
            return taskRelationshipRepository.findAll(spec);
        } else {
            return taskRelationshipRepository.findAll();
        }
    }
}