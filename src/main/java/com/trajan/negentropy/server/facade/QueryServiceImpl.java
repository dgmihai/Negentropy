package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.backend.repository.NetDurationRepository;
import com.trajan.negentropy.server.backend.sync.SyncManager;
import com.trajan.negentropy.server.facade.response.Response.SyncResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Transactional
@Benchmark
public class QueryServiceImpl implements QueryService {
    
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private TagService tagService;
    @Autowired private NetDurationRepository timeRepository;
    @Autowired private SyncManager syncManager;
    @Autowired private DataContext dataContext;

    @Override
    public Task fetchTask(TaskID taskId) {
        return dataContext.toDO(entityQueryService.getTask(taskId));
    }

    @Override
    public Stream<Task> fetchTasks(Collection<TaskID> taskIds) {
        return entityQueryService.getTasks(taskIds)
                .map(dataContext::toDO);
    }


    @Override
    public TaskNode fetchNode(LinkID linkId) {
        return dataContext.toDO(entityQueryService.getLink(linkId));
    }

    @Override
    public Stream<TaskNode> fetchNodes(Collection<LinkID> linkIds) {
        return entityQueryService.getLinks(linkIds)
                .map(dataContext::toDO);
    }

    @Override
    public Stream<LinkID> fetchAllNodesByFilter(TaskFilter filter) {
        return entityQueryService.findLinkIds(filter);
    }


    @Override
    public Stream<Task> fetchTasks(TaskFilter filter) {
        return entityQueryService.findTasks(filter)
                .map(dataContext::toDO);
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
    public int fetchChildCount(TaskID parentTaskId, TaskFilter filter, int offset, int limit) {
        return entityQueryService.findChildCount(parentTaskId, filter, offset, limit);
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
                .map(dataContext::toDO);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskFilter filter, int offset, int limit) {
        return entityQueryService.findChildLinks(parentTaskId, filter, offset, limit)
                .map(dataContext::toDO);
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
    public int fetchRootCount(TaskFilter filter, int offset, int limit) {
        return entityQueryService.findChildCount(null, filter, offset, limit);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes() {
        return this.fetchRootNodes(null);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes(TaskFilter filter) {
        return entityQueryService.findChildLinks(null, filter)
                .map(dataContext::toDO);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes(TaskFilter filter, int offset, int limit) {
        return entityQueryService.findChildLinks(null, filter, offset, limit)
                .map(dataContext::toDO);
    }

    @Override
    public Stream<TaskNode> fetchDescendantNodes(TaskID ancestorId, TaskFilter filter) {
        return entityQueryService.findDescendantLinks(ancestorId, filter)
                .map(dataContext::toDO);
    }

    @Override
    public Stream<LinkID> fetchDescendantNodeIds(TaskID ancestorId, TaskFilter filter) {
        return fetchDescendantNodes(ancestorId, filter)
                .map(TaskNode::id);
    }

    @Override
    public Tag fetchTag(TagID tagId) {
        return dataContext.toDO(tagService.getTagEntity(tagId));
    }

    @Override
    public Tag fetchTagByName(String tagName) {
        Optional<TagEntity> tagEntity = tagService.getTagEntityByName(tagName);
        return tagEntity.map(dataContext::toDO).orElse(null);
    }


    @Override
    public Stream<Tag> fetchAllTags() {
        return tagService.findAll().stream()
                .map(dataContext::toDO);
    }

    @Override
    public Stream<Tag> fetchTags(TaskID taskId) {
        return entityQueryService.getTask(taskId).tags().stream()
                .map(dataContext::toDO);
    }

    @Override
    public Map<TaskID, Duration> fetchAllNetDurations(TaskFilter filter) {
        return entityQueryService.getAllNetDurations(filter);
    }

    @Override
    public Duration fetchNetDuration(TaskID taskId, TaskFilter filter) {
        return entityQueryService.calculateNetDuration(taskId, filter);
        // TODO: Cache: return this.fetchNetTimeDuration(taskId, 0);
    }

    @Override
    public int fetchLowestImportanceOfDescendants(TaskID ancestorId) {
        return entityQueryService.getLowestImportanceOfDescendants(ancestorId);
    }

    @Override
    public SyncResponse sync(SyncID syncId) {
        return new SyncResponse(true, "Synced", syncManager.aggregatedSyncRecord(syncId));
    }

    @Override
    public SyncID currentSyncId() {
        return this.sync(null).aggregateSyncRecord().id();
    }
}
