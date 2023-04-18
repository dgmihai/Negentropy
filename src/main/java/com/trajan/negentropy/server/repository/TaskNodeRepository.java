package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository("taskNodeRepository")
@Transactional
public interface TaskNodeRepository extends JpaRepository<TaskNode, Long>, JpaSpecificationExecutor<TaskNode>, GenericSpecificationProvider<TaskNode> {
    // Find all TaskNode entities matching the specified filters
    default List<TaskNode> findAllFiltered(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return findAll();
        } else {
            Specification<TaskNode> spec = getSpecificationFromFilters(filters, TaskNode.class);
            return new ArrayList<>(findAll(spec, Sort.unsorted()));
        }
    }

    // Find all TaskNode entities with the specified parent
    List<TaskNode> findByParentTask(Task parent);

    // Find all TaskNode entities with the specified parent
    List<TaskNode> findByReferenceTask(Task reference);

    // Find all TaskNode entities with the specified parent that match the specified filters
    default List<TaskNode> findByParentFiltered(Task parent, List<Filter> filters) {
        List<TaskNode> nodes = findByParentTask(parent);
        List<TaskNode> orderedNodes = orderNodes(nodes);
        return applyFilters(orderedNodes, filters);
    }

    // Count the number of children of the specified TaskNode
    int countByParentTask(Task parent);

    // Helper method to apply filters to a list of TaskNode entities
    default List<TaskNode> applyFilters(List<TaskNode> nodes, List<Filter> filters) {
        if (filters == null || filters.isEmpty()) {
            return nodes;
        } else {
            Specification<TaskNode> spec = getSpecificationFromFilters(filters, TaskNode.class);
            return new ArrayList<>(findAll(spec.and((root, query, builder) ->
                    builder.isTrue(builder.literal(true))), Sort.unsorted()));
        }
    }

    default List<TaskNode> orderNodes(List<TaskNode> unorderedChildren) {
        List<TaskNode> orderedChildren = new ArrayList<>();
        if (!unorderedChildren.isEmpty()) {
            TaskNode head = null;
            for (TaskNode child : unorderedChildren) {
                if (child.getPrev() == null) {
                    head = child;
                    break;
                }
            }
            if (head != null) {
                TaskNode current = head;
                while (current != null) {
                    orderedChildren.add(current);
                    current = current.getNext();
                }
            } else throw new RuntimeException("Fetched child nodes are malformed.");
        }
        return orderedChildren;
    }
}
