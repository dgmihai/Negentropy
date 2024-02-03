package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.NetDurationService.NetDurationInfo;
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
import java.util.stream.StreamSupport;

@Service
@Transactional
@Benchmark(millisFloor = 10)
public class QueryServiceImpl implements QueryService {
    
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private TagService tagService;
    @Autowired private NetDurationRepository timeRepository;
    @Autowired private SyncManager syncManager;
    @Autowired private DataContext dataContext;
    @Autowired private NetDurationService netDurationService;

    @Override
    public Task fetchTask(TaskID taskId) {
        return dataContext.toEagerDO(entityQueryService.getTask(taskId));
    }

    @Override
    public Stream<Task> fetchTasks(Collection<TaskID> taskIds) {
        return entityQueryService.getTasks(taskIds)
                .map(dataContext::toEagerDO);
    }

    @Override
    public TaskNode fetchNode(LinkID linkId) {
        return dataContext.toEagerDO(entityQueryService.getLink(linkId));
    }

    @Override
    public Stream<TaskNode> fetchNodes(Collection<LinkID> linkIds) {
        return entityQueryService.getLinks(linkIds)
                .map(dataContext::toEagerDO);
    }

    @Override
    public Stream<TaskNode> fetchAllNodes(TaskNodeTreeFilter filter) {
        return entityQueryService.findLinks(filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<LinkID> fetchAllNodesAsIds(TaskNodeTreeFilter filter) {
        return entityQueryService.findLinks(filter)
                .map(ID::of);
    }

    @Override
    public Stream<TaskNode> fetchAllNodesNested(TaskNodeTreeFilter filter) {
        return entityQueryService.findLinksNested(filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<LinkID> fetchAllNodesNestedAsIds(TaskNodeTreeFilter filter) {
        return entityQueryService.findLinksNested(filter)
                .map(ID::of);
    }

    @Override
    public Stream<Task> fetchAllTasks(TaskTreeFilter filter) {
        return entityQueryService.findTasks(filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public int fetchChildCount(TaskID parentTaskId) {
        return this.fetchChildCount(parentTaskId, null);
    }

    @Override
    public int fetchChildCount(TaskID parentTaskId, TaskNodeTreeFilter filter) {
        return entityQueryService.findChildCount(parentTaskId, filter);
    }

    @Override
    public int fetchChildCount(TaskID parentTaskId, TaskNodeTreeFilter filter, int offset, int limit) {
        return entityQueryService.findChildCount(parentTaskId, filter, offset, limit);
    }

    @Override
    public boolean hasChildren(TaskID parentTaskId) {
        return this.hasChildren(parentTaskId, null);
    }

    @Override
    public boolean hasChildren(TaskID parentTaskId, TaskNodeTreeFilter filter) {
        return entityQueryService.hasChildren(parentTaskId, filter);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId) {
        return this.fetchChildNodes(parentTaskId, null);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskNodeTreeFilter filter) {
        return entityQueryService.findChildLinks(parentTaskId, filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<TaskNode> fetchChildNodes(TaskID parentTaskId, TaskNodeTreeFilter filter, int offset, int limit) {
        return entityQueryService.findChildLinks(parentTaskId, filter, offset, limit)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes() {
        return this.fetchRootNodes(null);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes(TaskNodeTreeFilter filter) {
        return entityQueryService.findChildLinks(null, filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<TaskNode> fetchRootNodes(TaskNodeTreeFilter filter, int offset, int limit) {
        return entityQueryService.findChildLinks(null, filter, offset, limit)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<TaskNode> fetchNodesGroupedHierarchically(TaskNodeTreeFilter filter) {
        return entityQueryService.findLinksGroupedHierarchically(filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<TaskNode> fetchDescendantNodes(TaskID ancestorId, TaskNodeTreeFilter filter) {
        return entityQueryService.findDescendantLinks(ancestorId, filter)
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<LinkID> fetchDescendantNodeIds(TaskID ancestorId, TaskNodeTreeFilter filter) {
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
        return tagService.getTagsForTask(taskId)
                .map(dataContext::toDO);
    }

    @Override
    public Collection<TaskID> fetchTaskIdsByTagId(TagID tagId) {
        return tagService.getTaskIdByTagId(tagId);
    }

    @Override
    public Stream<TaskNode> fetchNodesThatHaveActiveRoutineSteps() {
        return entityQueryService.findLinksThatHaveActiveRoutineSteps()
                .map(dataContext::toLazyDO);
    }

    @Override
    public Stream<Routine> fetchActiveRoutines() {
        return StreamSupport.stream(entityQueryService.findActiveRoutines()
                        .spliterator(), false)
                .map(dataContext::toDO);
    }

    @Override
    public Map<LinkID, Duration> fetchAllNetNodeDurations(TaskNodeTreeFilter filter) {
        return netDurationService.getAllNetNodeDurations(filter);
    }

    @Override
    public Map<TaskID, Duration> fetchAllNetTaskDurations(TaskNodeTreeFilter filter) {
        return netDurationService.getAllNetTaskDurations(filter);
    }

    @Override
    public NetDurationInfo fetchNetDurationInfo(TaskNodeTreeFilter filter) {
        return netDurationService.getNetDurationInfo(filter);
    }

    @Override
    public Duration fetchNetDuration(TaskID taskId, TaskNodeTreeFilter filter) {
        return netDurationService.getNetDuration(taskId, filter);
    }

    @Override
    public int fetchLowestImportanceOfDescendants(TaskID ancestorId) {
        return entityQueryService.getLowestImportanceOfDescendants(ancestorId);
    }

    @Override
    public SyncResponse sync(SyncID syncId) {
        return new SyncResponse(true, null, syncManager.aggregatedSyncRecord(syncId));
    }

    @Override
    public SyncID currentSyncId() {
        return this.sync(null).aggregateSyncRecord().id();
    }
}
