package com.trajan.negentropy.server.backend;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.backend.repository.TimeEstimateRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@Transactional
public class TaskEntityQueryServiceImpl implements TaskEntityQueryService {
    @Autowired private TaskRepository taskRepository;

    @Autowired private LinkRepository linkRepository;

    @Autowired private TimeEstimateRepository timeRepository;

    @Override
    public TaskEntity getTask(long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() ->
                new IllegalArgumentException("Failed to get task with id " + taskId + " from repository"));
    }

    @Override
    public TaskLink getLink(long linkId) {
        return linkRepository.findById(linkId).orElseThrow(() ->
                new IllegalArgumentException("Failed to get link with id " + linkId +
                        " from repository."));
    }

    @Override
    public Stream<TaskEntity> findTasks(Iterable<Filter> filters, Iterable<TagEntity> tags) {
        if (Iterables.isEmpty(tags)) {
            return taskRepository.findAllFiltered(filters).stream();
        } else {
            return taskRepository.findAllFilteredAndTagged(filters, tags).stream().unordered();
        }
    }

    @Override
    public int getChildCount(Long parentId) {
        return parentId == null ?
                linkRepository.countByParentIsNull() :
                linkRepository.countByParentId(parentId);
    }

    @Override
    public boolean hasChildren(long parentId) {
        return linkRepository.existsByParentId(parentId);
    }

    @Override
    public Stream<TaskLink> getLinksByChild(TaskEntity child) {
        return linkRepository.findByChild(child);
    }

    @Override
    public Stream<TaskLink> getLinksByParent(TaskEntity parent) {
        return linkRepository.findByParentOrderByPositionAsc(parent);
    }

    @Override
    public Stream<TaskLink> getLinksByParentId(long parentId) {
        return linkRepository.findByParentIdOrderByPositionAsc(parentId);
    }

    @Override
    public boolean hasParents(TaskEntity child) {
        return linkRepository.existsByChildAndParentNotNull(child);
    }

    @Override
    public Stream<TaskLink> getAncestorLinks(TaskEntity descendant) {
        List<TaskLink> ancestors = new ArrayList<>();
        this.collectAncestors(descendant, ancestors);
        return ancestors.stream().unordered();
    }

    @Override
    public Stream<TaskEntity> getAncestors(TaskEntity descendant) {
        return this.getAncestorLinks(descendant)
                .map(TaskLink::parent);
    }

    private void collectAncestors(TaskEntity child, List<TaskLink> ancestors) {
        if (child != null) {
            for (TaskLink link : child.parentLinks()) {
                ancestors.add(link);
                collectAncestors(link.parent(), ancestors);
            }
        }
    }

    @Override
    public Stream<TaskEntity> getDescendants(TaskEntity ancestor) {
        return this.getDescendantLinks(ancestor)
                .map(TaskLink::child);
    }

    @Override
    public Stream<TaskLink> getDescendantLinks(TaskEntity ancestor) {
        List<TaskLink> descendants = new ArrayList<>();
        this.collectDescendants(ancestor, descendants);
        return descendants.stream();
    }

    private void collectDescendants(TaskEntity parent, List<TaskLink> descendants) {
        if (parent != null) {
            for (TaskLink link : parent.childLinks()) {
                descendants.add(link);
                collectDescendants(link.child(), descendants);
            }
        }
    }

    @Override
    public Duration getEstimatedTotalDuration(long taskId) {
        // TODO: Implement
        return Duration.ZERO;
//        return timeRepository.findOneByTaskId(taskId).duration();
    }

}
