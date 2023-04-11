package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskRelationship;
import com.trajan.negentropy.server.entity.TaskRelationship_;
import com.trajan.negentropy.server.repository.Filter;
import com.trajan.negentropy.server.repository.FilteredTaskRelationshipRepository;
import com.trajan.negentropy.server.repository.QueryOperator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Transactional
@Service("taskRelationshipService")
public class TaskRelationshipService implements GenericDataService<TaskRelationship> {
    private static final Logger logger = LoggerFactory.getLogger(TaskRelationshipService.class);

    private final FilteredTaskRelationshipRepository filteredTaskRelationshipRepository;

    public TaskRelationshipService(FilteredTaskRelationshipRepository filteredTaskRelationshipRepository) {
        this.filteredTaskRelationshipRepository = filteredTaskRelationshipRepository;
    }

    @Override
    public Response save(TaskRelationship entity) {
        throw new UnsupportedOperationException("We should never be directly saving a TaskRelationship!");
    }

    @Override
    public TaskRelationship findById(Long id) {
        return filteredTaskRelationshipRepository.getTaskRelationshipRepository().findById(id).orElse(null);
    }

    @Override
    public List<TaskRelationship> find(List<Filter> filters) {
        return filteredTaskRelationshipRepository.findByFilters(filters);
    }

    @Override
    public Response delete(TaskRelationship entity) {
        try {
            filteredTaskRelationshipRepository.getTaskRelationshipRepository().delete(entity);
            return new Response();
        } catch (Exception e) {
            return new Response(e);
        }
    }

    @Override
    public List<TaskRelationship> getAll() {
        return filteredTaskRelationshipRepository.getTaskRelationshipRepository().findAll();
    }

    public List<TaskRelationship> findRoots() {
        Filter rootFilter = Filter.builder()
                .field(TaskRelationship_.PARENT_RELATIONSHIP)
                .operator(QueryOperator.NULL)
                .build();

        List<Filter> filters = new ArrayList<>();
        filters.add(rootFilter);

        return find(filters);
    }
}