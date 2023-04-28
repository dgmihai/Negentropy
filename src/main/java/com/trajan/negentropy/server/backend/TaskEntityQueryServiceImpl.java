package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.backend.repository.TimeEstimateRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

@Service
public class TaskEntityQueryServiceImpl implements TaskEntityQueryService {
    @Autowired
    TaskRepository taskRepository;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    TimeEstimateRepository timeRepository;

    @Override
    public TaskEntity getTask(long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() ->
                new IllegalArgumentException("Failed to get task with id " + taskId + " from repository"));
    }

    @Override
    public TaskLinkEntity getLink(long linkId) {
        return linkRepository.findById(linkId).orElseThrow(() ->
                new IllegalArgumentException("Failed to get link with id " + linkId +
                        " from repository."));
    }

    @Override
    public Stream<TaskEntity> findTasks(List<Filter> filters, Set<TagEntity> tags) {
        if (tags.isEmpty()) {
            return taskRepository.findAllWithFilters(filters).stream();
        } else {
            return taskRepository.findAllWithFiltersAndTags(filters, tags).stream().unordered();
        }
    }

    @Override
    public int countChildren(TaskEntity parent) {
        return parent.childLinks().size();
    }

    @Override
    public boolean hasChildren(TaskEntity parent) {
        return linkRepository.existsByParent(parent);
    }

    @Override
    public Stream<TaskLinkEntity> getLinksByChild(TaskEntity child) {
        return linkRepository.findByChild(child);
    }

    @Override
    public Stream<TaskLinkEntity> getLinksByParent(TaskEntity parent) {
        return linkRepository.findByParent(parent);
    }

    @Override
    public boolean hasParents(TaskEntity child) {
        return linkRepository.existsByChildAndParentNotNull(child);
    }

    @Override
    public Stream<TaskLinkEntity> getAncestorLinks(TaskEntity descendant) {
        List<TaskLinkEntity> ancestors = new ArrayList<>();
        this.collectAncestors(descendant, ancestors);
        return ancestors.stream().unordered();
    }

    @Override
    public Stream<TaskEntity> getAncestors(TaskEntity descendant) {
        return this.getAncestorLinks(descendant)
                .map(TaskLinkEntity::parent);
    }

    private void collectAncestors(TaskEntity child, List<TaskLinkEntity> ancestors) {
        if (child != null) {
            for (TaskLinkEntity link : child.parentLinks()) {
                ancestors.add(link);
                collectAncestors(link.parent(), ancestors);
            }
        }
    }

    @Override
    public Stream<TaskEntity> getDescendants(TaskEntity ancestor) {
        return this.getDescendantLinks(ancestor)
                .map(TaskLinkEntity::child);
    }

    @Override
    public Stream<TaskLinkEntity> getDescendantLinks(TaskEntity ancestor) {
        List<TaskLinkEntity> descendants = new ArrayList<>();
        this.collectDescendants(ancestor, descendants);
        return descendants.stream();
    }

    private void collectDescendants(TaskEntity parent, List<TaskLinkEntity> descendants) {
        if (parent != null) {
            for (TaskLinkEntity link : parent.childLinks()) {
                descendants.add(link);
                collectDescendants(link.child(), descendants);
            }
        }
    }

    @Override
    public Duration getEstimatedTotalDuration(long taskId) {
        return timeRepository.findOneByTaskId(taskId).duration();
    }

}
