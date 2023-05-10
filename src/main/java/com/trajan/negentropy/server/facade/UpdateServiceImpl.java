package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.LinkID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import com.trajan.negentropy.server.facade.response.NodeResponse;
import com.trajan.negentropy.server.facade.response.Response;
import com.trajan.negentropy.server.facade.response.TagResponse;
import com.trajan.negentropy.server.facade.response.TaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateServiceImpl implements UpdateService {
    private static final Logger logger = LoggerFactory.getLogger(UpdateServiceImpl.class);

    @Autowired private DataContext dataContext;
    @Autowired private EntityQueryService entityQueryService;

    private final String OK = "OK";

    @Override
    public NodeResponse insertTaskNode(TaskNodeDTO fresh) {
        try {
            logger.debug("Inserting task node " + fresh);

            TaskLink freshLink = dataContext.mergeNode(fresh);

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

            TaskEntity taskEntity = dataContext.mergeTask(task);

            return new TaskResponse(true, taskEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TaskResponse(false, null, e.getMessage());
        }
    }

    @Override
    public TaskResponse updateTask(Task task) {
        try {
            logger.debug("Updating task " + task.name());

            TaskEntity taskEntity = dataContext.mergeTask(task);

            return new TaskResponse(true, taskEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TaskResponse(false, null, e.getMessage());
        }
    }

    @Override
    public NodeResponse updateNode(TaskNode node) {
        try {
            logger.debug("Updating link " + node.linkId());

            TaskLink link = dataContext.mergeNode(node);

            return new NodeResponse(true, link, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new NodeResponse(false, null, e.getMessage());
        }
    }

    @Override
    public Response deleteTask(TaskID taskId) {
        throw new RuntimeException("NOT YET IMPLEMENTED");
//        TaskEntity task = entityQueryService.getTask(taskId());
//        logger.debug("Deleting task " + task);
//        try {
//            dataContext.deleteTask(task);
////
//            return new Response(true, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Response(false, e.getMessage());
//        }
    }

    @Override
    public Response deleteNode(LinkID linkId) {
        try {
            logger.debug("Deleting link " + linkId);

            TaskLink link = entityQueryService.getLink(linkId);

            dataContext.deleteLink(link);

            return new Response(true, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, e.getMessage());
        }
    }

    @Override
    public TagResponse createTag(Tag tag) {
        try {
            logger.debug("Creating tag " + tag.name());

            TagEntity tagEntity = dataContext.mergeTag(tag);

            return new TagResponse(true, tagEntity, this.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new TagResponse(false, null, e.getMessage());
        }
    }
}
