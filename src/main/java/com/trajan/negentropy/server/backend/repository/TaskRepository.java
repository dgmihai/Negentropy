package com.trajan.negentropy.server.backend.repository;

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
import java.util.Set;

/**
 * A repository interface for managing Task entities.
 */
@Repository("taskRepository")
@Transactional
public interface TaskRepository extends JpaRepository<TaskEntity, Long>, JpaSpecificationExecutor<TaskEntity>, GenericSpecificationProvider<TaskEntity> {

    /**
     * Finds all Task entities matching the specified filters.
     *
     * @param filters A list of filters to be applied.
     * @return A list of Task entities matching the filters.
     */
    default List<TaskEntity> findAllWithFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TaskEntity> spec = getSpecificationFromFilters(filters, TaskEntity.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }


    default List<TaskEntity> findAllWithFiltersAndTags(List<Filter> filters, Set<TagEntity> tags) {
        Specification<TaskEntity> spec = Specification.where(this.hasAnyTags(tags));
        if (filters.size() > 0) {
            Specification<TaskEntity> filterSpec = getSpecificationFromFilters(filters, TaskEntity.class);
            spec = spec.and(filterSpec);
        }
        return findAll(spec);
    }

   default Specification<TaskEntity> hasAnyTags(Set<TagEntity> tags) {
        return (root, query, builder) -> {
            Join<TaskEntity, TagEntity> tagJoin = root.join("tags", JoinType.INNER);
            return tagJoin.in(tags);
        };
    }
}
