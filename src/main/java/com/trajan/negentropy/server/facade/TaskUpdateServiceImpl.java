package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLinkEntity;
import com.trajan.negentropy.server.facade.response.LinkResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import com.trajan.negentropy.server.facade.model.EntityMapper;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class TaskUpdateServiceImpl implements TaskUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateServiceImpl.class);

    @Autowired
    DataContext dataContext;

    @Autowired
    EntityMapper mapper;

    @Autowired
    TaskEntityQueryService entityQueryService;

    private final String OK = "OK";
    @Override
    public LinkResponse addTaskAsChildAt(int index, long parentId, Task child) {
        TaskEntity parent = null;
        TaskEntity freshTask = null;
        TaskLinkEntity freshLink = null;

        logger.debug("Saving task " + child + " as subtask of parent with id " + parentId);
        try {
            parent = entityQueryService.getTask(parentId);
            freshTask = mapper.toEntity(child);

            freshLink = this.createLinkToParent(index, freshTask, parent);

            // TODO: Update time estimate for all ancestors if duration changed

            return new LinkResponse(true, parent, freshTask, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();

            return new LinkResponse(false, parent, freshTask, freshLink, e.getMessage());
        }
    }

    @Override
    public LinkResponse addTaskAsChild(long parentId, Task child) {
        return addTaskAsChildAt(-1, parentId, child);
    }

    private TaskLinkEntity createLinkToParent(int index, TaskEntity freshTask, TaskEntity parent) {
        TaskLinkEntity freshLink = new TaskLinkEntity().toBuilder()
                .parent(parent)
                .child(freshTask)
                .build();
        freshLink = dataContext.createLink(freshLink);

        if (index == -1) {
            logger.debug("Creating link for task " + freshTask + " as a child of " + parent);
            index = parent.childLinks().size();
        } else {
            logger.debug("Creating link for task " + freshTask + " as a child of " + parent + " at index " + index);
            for (TaskLinkEntity link : parent.childLinks()) {
                if (link.position() >= index) {
                    link.position(link.position() + 1);
                }
            }
        }

        freshLink.position(index);
        parent.childLinks().add(index, freshLink);
        freshTask.parentLinks().add(freshLink);

        return freshLink;
    }

    @Override
    public LinkResponse addTaskAsRoot(Task task) {
        TaskEntity freshTask = null;
        TaskLinkEntity freshLink = null;

        logger.debug("Saving task " + task + " as a root task");
        try {
            freshTask = mapper.toEntity(task);
            freshLink = this.createLinkToRoot(freshTask);

            return new LinkResponse(true, null, freshTask, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkResponse(false, null, freshTask, freshLink, e.getMessage());
        }
    }

    private TaskLinkEntity createLinkToRoot(TaskEntity fresh) {
        TaskLinkEntity freshLink = new TaskLinkEntity().toBuilder()
                .child(fresh)
                .build();
        freshLink = dataContext.createLink(freshLink);

        fresh.parentLinks().add(freshLink);
        return freshLink;
    }

    @Override
    public TaskResponse updateTask(Task task) {
        TaskEntity taskEntity = null;
        logger.debug("Updating task " + task.name());
        try {
            if (task.id() == null) {
                throw new IllegalArgumentException("Attempted to update Task " + task +
                        " that doesn't exist in the repository.");
            }
            taskEntity = mapper.toEntity(task);

            // TODO: Update time estimate for all ancestors if duration changed

            return new TaskResponse(true, taskEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TaskResponse(false, taskEntity, e.getMessage());
        }
    }

    @Override
    public Response deleteTask(long taskId) {
        TaskEntity task = entityQueryService.getTask(taskId);
        logger.debug("Deleting task " + task);
        try {
            dataContext.deleteTask(task);

            // TODO: Update time estimate for all ancestors if duration changed

            return new Response(true, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, e.getMessage());
        }
    }

    @Override
    public LinkResponse deleteLink(long linkId) {
        TaskEntity parent = null;
        TaskEntity child = null;

        try {
            TaskLinkEntity link = entityQueryService.getLink(linkId);

            parent = link.parent();
            child = link.child();

            logger.debug("Deleting link " + link);
            parent.childLinks().remove(link);
            child.parentLinks().remove(link);

            // TODO: Update time estimate for all ancestors if duration changed

            return new LinkResponse(true, parent, child, null, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkResponse(false, parent, child, null, e.getMessage());
        }
    }
}
