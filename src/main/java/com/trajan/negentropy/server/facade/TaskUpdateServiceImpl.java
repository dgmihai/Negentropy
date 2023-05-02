package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.TaskEntityQueryService;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.*;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class TaskUpdateServiceImpl implements TaskUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(TaskUpdateServiceImpl.class);

    @Autowired private DataContext dataContext;
    @Autowired private TaskEntityQueryService entityQueryService;

    private final String OK = "OK";

    private TaskLink createLinkToRoot(TaskEntity fresh) {
        TaskLink freshLink = new TaskLink()
                .child(fresh);
        freshLink = dataContext.createLink(freshLink);

        fresh.parentLinks().add(freshLink);
        return freshLink;
    }

    @Override
    public NodeResponse insertTaskAsRoot(TaskID freshId) {
        try {
            TaskEntity fresh = entityQueryService.getTask(freshId.val());

            logger.debug("Inserting task " + fresh + " as a root task");

            TaskLink freshLink = this.createLinkToRoot(fresh);

            return new NodeResponse(true, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new NodeResponse(false, null, e.getMessage());
        }
    }

    private TaskLink createLinkToParent(int index, TaskEntity parent, TaskEntity child) {
        if (parent.equals(child)) {
            throw new IllegalArgumentException("Cannot add task as a child of self");
        }

        if (entityQueryService.getAncestors(parent)
                .filter(Objects::nonNull)
                .anyMatch(task -> task.equals(parent) || task.equals(child))) {
            throw new IllegalArgumentException("Cannot create link between " + parent + " and " + child +
                    "; would create cyclic hierarchy.");
        }

        TaskLink freshLink = new TaskLink()
                .parent(parent)
                .child(child);

        freshLink = dataContext.createLink(freshLink);

        if (index == -1) {
            logger.debug("Creating link for task " + child + " as a child of " + parent);
            index = parent.childLinks().size();
        } else {
            logger.debug("Creating link for task " + child + " as a child of " + parent + " at index " + index);
            for (TaskLink link : parent.childLinks()) {
                if (link.position() >= index) {
                    link.position(link.position() + 1);
                }
            }
        }

        freshLink.position(index);
        parent.childLinks().add(index, freshLink);
        child.parentLinks().add(freshLink);

        return freshLink;
    }

    @Override
    public NodeResponse insertTaskAsChild(TaskID parentId, TaskID childId) {
        return insertTaskAsChildAt(-1, parentId, childId);
    }

    @Override
    public NodeResponse insertTaskAsChildAt(int index, TaskID parentId, TaskID childId) {
        try {
            if (parentId == null) {
                return this.insertTaskAsRoot(childId);
            }

            TaskEntity parent = entityQueryService.getTask(parentId.val());
            TaskEntity child = entityQueryService.getTask(childId.val());

            logger.debug("Saving task " + child + " as subtask of parent " + parent);

            TaskLink freshLink = this.createLinkToParent(index, parent, child);

            // TODO: Update time estimate for all ancestors if duration changed

            return new NodeResponse(true, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();

            return new NodeResponse(false, null, e.getMessage());
        }
    }

    @Override
    public NodeResponse insertTaskBefore(TaskID freshId, LinkID nextId) {
        try {
            TaskEntity fresh = entityQueryService.getTask(freshId.val());
            TaskLink next = entityQueryService.getLink(nextId.val());

            logger.debug("Saving task " + fresh + " before  " + next);

            TaskLink freshLink = this.createLinkToParent(next.position(), next.parent(), fresh);

            // TODO: Update time estimate for all ancestors if duration changed

            return new NodeResponse(true, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();

            return new NodeResponse(false, null, e.getMessage());
        }
    }

    @Override
    public NodeResponse insertTaskAfter(TaskID freshId, LinkID prevId) {
        try {
            TaskEntity fresh = entityQueryService.getTask(freshId.val());
            TaskLink prev = entityQueryService.getLink(prevId.val());

            logger.debug("Saving task " + fresh + " after  " + prev);

            TaskLink freshLink = this.createLinkToParent(prev.position() + 1, prev.parent(), fresh);

            // TODO: Update time estimate for all ancestors if duration changed

            return new NodeResponse(true, freshLink, this.OK);
        } catch (Exception e) {
            e.printStackTrace();

            return new NodeResponse(false, null, e.getMessage());
        }
    }

    @Override
    public TaskResponse createTask(Task task) {
        try {
            logger.debug("Creating task " + task.name());
            TaskEntity taskEntity = dataContext.createTask(new TaskEntity(task.name()));

            taskEntity = EntityMapper.merge(task, taskEntity);

            // TODO: Update time estimate for all ancestors if duration changed

            return new TaskResponse(true, taskEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TaskResponse(false, null, e.getMessage());
        }
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
            taskEntity = entityQueryService.getTask(task.id().val());

            taskEntity = EntityMapper.merge(task, taskEntity);

            // TODO: Update time estimate for all ancestors if duration changed

            return new TaskResponse(true, taskEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TaskResponse(false, taskEntity, e.getMessage());
        }
    }

    @Override
    public NodeResponse updateNode(TaskNode node) {
        TaskLink link = null;
        logger.debug("Updating link " + node.linkId());
        try {
            if (node.linkId() == null) {
                throw new IllegalArgumentException("Attempted to update node " + node +
                        " that doesn't exist in the repository.");
            }
            link = entityQueryService.getLink(node.linkId().val());

            link = EntityMapper.merge(node, link);

            // TODO: Update time estimate for all ancestors if duration changed

            return new NodeResponse(true, link, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new NodeResponse(false, link, e.getMessage());
        }
    }

    @Override
    public Response deleteTask(TaskID taskId) {
        throw new RuntimeException("NOT YET IMPLEMENTED");
//        TaskEntity task = entityQueryService.getTask(taskId.val());
//        logger.debug("Deleting task " + task);
//        try {
//            dataContext.deleteTask(task);
//
//            // TODO: Update time estimate for all ancestors if duration changed
//
//            return new Response(true, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Response(false, e.getMessage());
//        }
    }

    @Override
    public Response deleteNode(LinkID linkId) {
        // TODO: Cleanup check if node is orphaned, dicated by paraemeter

        try {
            TaskLink link = entityQueryService.getLink(linkId.val());

            TaskEntity parent = link.parent();
            TaskEntity child = link.child();

            int index = link.position();

            logger.debug("Deleting link " + link);
            if (parent != null) {
                parent.childLinks().remove(link);

                for (TaskLink l : parent.childLinks()) {
                    if (l.position() > index) {
                        l.position(l.position() - 1);
                    }
                }
            }

            child.parentLinks().remove(link);

            // TODO: Update time estimate for all ancestors if duration changed

            return new Response(true, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, e.getMessage());
        }
    }
}
