package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.backend.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A repository interface for managing links between tasks.
 * </p>
 * A TaskLink represents the relationship a Task has to other Tasks.
 */
@Repository
@Transactional
public interface LinkRepository extends JpaRepository<TaskLinkEntity, Long>, JpaSpecificationExecutor<TaskLinkEntity>, GenericSpecificationProvider<TaskLinkEntity> {

    /**
     * Finds all TaskLink entities matching the specified filters.
     *
     * @param filters A list of filters to be applied.
     * @return A list of TaskLink entities matching the filters.
     */
    default List<TaskLinkEntity> findAllWithFilters(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll();
        } else {
            Specification<TaskLinkEntity> spec = getSpecificationFromFilters(filters, TaskLinkEntity.class);
            return new ArrayList<>(findAll(spec, Sort.unsorted()));
        }
    }

    /**
     * Returns all orphan Task entities.
     *
     * @return A list of orphan Task entities.
     */
    List<TaskLinkEntity> findByParentIsNull();

    /**
     * Checks if the task has any child task links or not.
     *
     * @param parent The parent Task entity.
     * @return A boolean value indicating whether the Task has child tasks represented through task links.
     */
    boolean existsByParent(TaskEntity parent);

    /**
     * Checks if the task is a child to any other task.
     *
     * @param child The child Task entity.
     * @return A boolean value indicating whether the Task is a child to any other Task.
     */
    boolean existsByChildAndParentNotNull(TaskEntity child);

    /**
     * Helper method to apply filters to a list of TaskLink entities.
     *
     * @param nodes The list of TaskLink entities to be filtered.
     * @param filters A list of filters to be applied.
     * @return A list of TaskLink entities that match the filters.
     */
    default List<TaskLinkEntity> applyFilters(List<TaskLinkEntity> nodes, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return nodes;
        } else {
            Specification<TaskLinkEntity> spec = getSpecificationFromFilters(filters, TaskLinkEntity.class);
            return new ArrayList<>(findAll(spec.and((root, query, builder) ->
                    builder.isTrue(builder.literal(true))), Sort.unsorted()));
        }
    }

    Stream<TaskLinkEntity> findByParent(TaskEntity parent);

    Stream<TaskLinkEntity> findByChild(TaskEntity child);
}
