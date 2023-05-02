package com.trajan.negentropy.server.backend.repository;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.backend.repository.filter.GenericSpecificationProvider;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * A repository interface for managing Task entities.
 */
@Repository("taskRepository")
@Transactional
public interface TaskRepository extends
        JpaRepository<TaskEntity, Long>,
        JpaSpecificationExecutor<TaskEntity>,
        GenericSpecificationProvider<TaskEntity> {

    /**
     * Finds all Task entities matching the specified filters.
     *
     * @param filters A list of filters to be applied.
     * @return A list of Task entities matching the filters.
     */
    @Override
    default List<TaskEntity> findAllFiltered(Iterable<Filter> filters) {
        if (!Iterables.isEmpty(filters)) {
            Specification<TaskEntity> spec = getSpecificationFromFilters(filters, TaskEntity.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }

    default List<TaskEntity> findAllFilteredAndTagged(Iterable<Filter> filters, Iterable<TagEntity> tags) {
        Specification<TaskEntity> spec = Specification.where(this.hasAnyTags(tags));
        if (Iterables.size(filters) > 0) {
            Specification<TaskEntity> filterSpec = getSpecificationFromFilters(filters, TaskEntity.class);
            spec = spec.and(filterSpec);
        }
        return findAll(spec);
    }

   default Specification<TaskEntity> hasAnyTags(Iterable<TagEntity> tags) {
        return (root, query, builder) -> {
            Join<TaskEntity, TagEntity> tagJoin = root.join("tags", JoinType.INNER);
            return tagJoin.in(tags);
        };
    }
}
