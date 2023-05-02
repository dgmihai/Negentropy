package com.trajan.negentropy.server.backend.repository;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
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
public interface LinkRepository extends JpaRepository<TaskLink, Long>,
        JpaSpecificationExecutor<TaskLink>,
        GenericSpecificationProvider<TaskLink> {

    /**
     * Finds all TaskLink entities matching the specified filters.
     *
     * @param filters A list of filters to be applied.
     * @return A list of TaskLink entities matching the filters.
     */
    @Override
    default List<TaskLink> findAllFiltered(Iterable<Filter> filters) {
        if (filters == null || Iterables.isEmpty(filters)) {
            return findAll();
        } else {
            Specification<TaskLink> spec = getSpecificationFromFilters(filters, TaskLink.class);
            return new ArrayList<>(findAll(spec, Sort.unsorted()));
        }
    }

    /**
     * Returns all orphan Task entities.
     *
     * @return A list of orphan Task entities.
     */
    List<TaskLink> findByParentIsNull();

    /**
     * Checks if the task, given its ID, has any child task links or not.
     *
     * @param parentId The ID of the parent Task entity.
     * @return A boolean value indicating whether the Task has child tasks represented through task links.
     */
    boolean existsByParentId(long parentId);

    /**
     * Counts the number of task links where a task is the parent.
     *
     * @param parentId The ID of parent Task entity.
     * @return A count of links where the task with the given ID is a parent.
     * */
    int countByParentId(long parentId);

    /**
     * Counts the number of task links where the parent is null.
     *
     * @return A count of links that have a null parent taskt.
     * */
    int countByParentIsNull();

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
    default List<TaskLink> applyFilters(List<TaskLink> nodes, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return nodes;
        } else {
            Specification<TaskLink> spec = getSpecificationFromFilters(filters, TaskLink.class);
            return new ArrayList<>(findAll(spec.and((root, query, builder) ->
                    builder.isTrue(builder.literal(true))), Sort.unsorted()));
        }
    }

    Stream<TaskLink> findByParentOrderByPositionAsc(TaskEntity parent);

    Stream<TaskLink> findByParentIdOrderByPositionAsc(long parentId);

    Stream<TaskLink> findByChild(TaskEntity child);
}
