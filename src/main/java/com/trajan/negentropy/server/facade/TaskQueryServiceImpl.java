package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.facade.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
public class TaskQueryServiceImpl implements TaskQueryService {

    @Autowired private TaskEntityQueryService entityQueryService;
    @Autowired private TagService tagService;

    @Override
    public Task getTask(TaskID taskId) {
        return EntityMapper.toDTO(entityQueryService.getTask(taskId.val()));
    }

    @Override
    public TaskNode getNode(LinkID linkId) {
        return EntityMapper.toDTO(entityQueryService.getLink(linkId.val()));
    }

    @Override
    public TaskNode getNodeByPosition(int position, TaskID parentTask) {
        // TODO: Not yet implemented
        return null;
    }

    @Override
    public Duration getEstimatedTotalDuration(TaskID taskId) {
        return entityQueryService.getEstimatedTotalDuration(taskId.val());
    }

    @Override
    public List<Task> findTasks(Iterable<Filter> filters, Iterable<Long> tagIds) {
        return entityQueryService.findTasks(filters, tagService.findAllById(tagIds))
                .map(EntityMapper::toDTO)
                .toList();
    }

    @Override
    public int getChildCount(TaskID parentTaskId) {
        return entityQueryService.getChildCount(parentTaskId.val());
    }

    @Override
    public boolean hasChildren(TaskID parentTaskId) {
        return entityQueryService.hasChildren(parentTaskId.val());
    }

    @Override
    public List<TaskNode> getChildNodes(TaskID parentTaskId) {
        return entityQueryService.getLinksByParentId(parentTaskId.val())
                .map(EntityMapper::toDTO)
                .toList();
    }

    @Override
    public int getRootCount() {
        return entityQueryService.getChildCount(null);
    }

    @Override
    public List<TaskNode> getRootNodes() {
        return entityQueryService.getLinksByParent(null)
                .map(EntityMapper::toDTO)
                .toList();
    }
}
