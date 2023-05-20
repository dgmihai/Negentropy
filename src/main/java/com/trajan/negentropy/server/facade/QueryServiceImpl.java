package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.backend.repository.TotalDurationEstimateRepository;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TagID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.stream.Stream;

@Service
@Transactional
public class QueryServiceImpl implements QueryService {
    
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private TagService tagService;
    @Autowired private TotalDurationEstimateRepository timeRepository;

    @Override
    public Task fetchTask(TaskID taskId) {
        return DataContext.toDTO(entityQueryService.getTask(taskId));
    }

    @Override
    public TaskNode fetchNode(LinkID linkId) {
        return DataContext.toDTO(entityQueryService.getLink(linkId));
    }

    @Override
    public Stream<Task> fetchTasks(TaskFilter filter) {
        return entityQueryService.findTasks(filter)
                .map(DataContext::toDTO);
    }

    @Override
    public int fetchChildCount(TaskID parentTaskId) {
        return this.fetchChildCount(parentTaskId, null);
    }

    @Override
    public int fetchChildCount(TaskID parentTaskId, TaskFilter filter) {
        return entityQueryService.findChildCount(parentTaskId, filter);
    }

    @Override
    public boolean hasChildren(TaskID parentTaskId) {
        return this.hasChildren(parentTaskId, null);
    }

    @Override
    public boolean hasChildren(TaskID parentTaskId, TaskFilter filter) {
        return entityQueryService.hasChildren(parentTaskId, filter);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId) {
        return this.fetchChildNodes(parentTaskId, null);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskFilter filter) {
        return entityQueryService.findChildLinks(parentTaskId, filter)
                .map(DataContext::toDTO);
    }

    @Override
    public int fetchRootCount() {
        return entityQueryService.findChildCount(null, null);
    }

    @Override
    public int fetchRootCount(TaskFilter filter) {
        return entityQueryService.findChildCount(null, filter);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes() {
        return this.fetchRootNodes(null);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes(TaskFilter filter) {
        return entityQueryService.findChildLinks(null, filter)
                .map(DataContext::toDTO);
    }

    @Override
    public Tag fetchTag(TagID tagId) {
        return DataContext.toDTO(tagService.getTagEntity(tagId));
    }

    @Override
    public Stream<Tag> fetchAllTags() {
        return tagService.findAll().stream()
                .map(DataContext::toDTO);
    }

    @Override
    public Stream<Tag> fetchTags(TaskID taskId) {
        return entityQueryService.getTask(taskId).tags().stream()
                .map(DataContext::toDTO);
    }

    @Override
    public Duration fetchNetTimeDuration(TaskID taskId) {
        return this.fetchNetTimeDuration(taskId, 0);
    }

    @Override
    public Duration fetchNetTimeDuration(TaskID taskId, int priority) {
        return entityQueryService.getTotalDuration(taskId, 0)
                .totalDuration();
    }

    @Override
    public Duration fetchNetTimeDuration(TaskID taskId, TaskFilter filter) {
        return entityQueryService.calculateTotalDuration(taskId, filter);
    }

    @Override
    public int fetchLowestImportanceOfDescendants(TaskID ancestorId) {
        return entityQueryService.getLowestImportanceOfDescendants(ancestorId);
    }
}
