package com.trajan.negentropy.server.facade;//package com.trajan.negentropy.server.facade;
//
//import com.trajan.negentropy.client.K;
//import com.trajan.negentropy.model.*;
//import com.trajan.negentropy.model.filter.TaskFilter;
//import com.trajan.negentropy.model.changes.ID;
//import com.trajan.negentropy.model.changes.LinkID;
//import com.trajan.negentropy.model.changes.TaskID;
//import com.trajan.negentropy.server.backend.DataContext;
//import com.trajan.negentropy.server.backend.EntityQueryService;
//import com.trajan.negentropy.model.entity.TagEntity;
//import com.trajan.negentropy.model.entity.TaskEntity;
//import com.trajan.negentropy.model.entity.TaskLink;
//import com.trajan.negentropy.server.backend.repository.TotalDurationEstimateRepository;
//import com.trajan.negentropy.server.facade.response.*;
//import com.trajan.negentropy.server.backend.sync.SyncManager;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Stream;
//
//@Service
//@Transactional
//public class UpdateServiceImpl implements UpdateService {
//    private static final Logger logger = LoggerFactory.getLogger(UpdateServiceImpl.class);
//
//    @Autowired private DataContext dataContext;
//    @Autowired private EntityQueryService entityQueryService;
//    @Autowired private TotalDurationEstimateRepository timeEstimateRepository;
//
//    private final SyncManager syncManager = SyncManager.get();
//    private final String OK = "OK";
//
//    @Override
//    public SyncResponse sync(UUID syncId) {
//        if (!syncManager.changesets().containsKey(syncId)) {
//            return new SyncResponseOld(false, "Sync ID not found", null, syncId);
//        } else {
//            Map<ChangeType, Set<ID>> changes = new HashMap<>();
//
//            List<SyncChangeset> values = syncManager.changesets().values();
//            SyncChangeset changesets = syncManager.changesets().get(syncId).get(0);
//            int index = values.indexOf(changesets);
//
//            List<SyncChangeset> subList = values.subList(index + 1, values.size());
//
//            subList.forEach(changeset -> {
//                changeset.changes().forEach(change -> {
//                    changes.computeIfAbsent(change.changeType(), k -> new HashSet<>())
//                            .add(change.changes());
//                });
//            });
//
//            return new SyncResponseOld(true, OK, changes, syncManager.currentSyncId());
//        }
//    }
//
//    @Override
//    public NodeResponse insertNode(Request<TaskNodeDTO> request) {
//        try {
//            syncManager.next();
//            logger.debug("Inserting task node " + fresh);
//
//            TaskLink freshLink = dataContext.mergeNode(fresh);
//
//            return new NodeResponse(true, freshLink, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new NodeResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public TaskResponse createTask(Task task) {
//        try {
//            syncManager.next();
//            logger.debug("Creating task " + task);
//
//            TaskEntity taskEntity = dataContext.mergeTask(task);
//
//            return new TaskResponse(true, taskEntity, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new TaskResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public TaskResponse updateTask(Task task) {
//        try {
//            syncManager.next();
//            logger.debug("Updating " + task);
//
//            TaskEntity taskEntity = dataContext.mergeTask(task);
//
//            return new TaskResponse(true, taskEntity, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new TaskResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public NodeResponse updateNode(TaskNode node) {
//        try {
//            syncManager.next();
//            logger.debug("Updating " + node);
//
//            TaskLink link = dataContext.mergeNode(node);
//
//            return new NodeResponse(true, link, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new NodeResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public NodesResponse updateNodes(Iterable<LinkID> linkIds, GenericTaskNodeInfo nodeInfo) {
//        try {
//            return tryUpdateNodes(linkIds, nodeInfo);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new NodesResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Transactional(rollbackFor = Exception.class)
//    private NodesResponse tryUpdateNodes(Iterable<LinkID> linkIds, GenericTaskNodeInfo nodeInfo) {
//        logger.debug("Updating multiple nodes with " + nodeInfo);
//
//        Deque<TaskNode> resultNodes = new LinkedList<>();
//
//        for (LinkID originalId : linkIds) {
//            TaskNode node = DataContext.toDTO(entityQueryService.getLink(originalId))
//                    .importance(nodeInfo.importance())
//                    .completed(nodeInfo.completed())
//                    .recurring(nodeInfo.recurring())
//                    .cron(nodeInfo.cron());
//
//            NodeResponse nodeResponse = this.updateNode(node);
//            if (!nodeResponse.success()) {
//                throw new RuntimeException("Failed to update node " + node);
//            }
//            resultNodes.add(nodeResponse.node());
//        }
//
//        return new NodesResponse(true, resultNodes.stream(), this.OK);
//    }
//
//    @Override
//    public Response deleteTask(TaskID taskId) {
//        throw new RuntimeException("NOT YET IMPLEMENTED");
////        TaskEntity task = entityQueryService.getTask(taskId());
////        logger.debug("Deleting task " + task);
////        try {
////            dataContext.deleteTask(task);
//////
////            return new Response(true, this.OK);
////        } catch (Exception e) {
////            e.printStackTrace();
////            return new Response(false, e.getMessage());
////        }
//    }
//
//    @Override
//    public Response deleteNode(LinkID originalId) {
//        try {
//            syncManager.next();
//            logger.debug("Deleting link " + originalId);
//
//            TaskLink link = entityQueryService.getLink(originalId);
//
//            dataContext.deleteLink(link);
//
//            return new Response(true, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Response(false, e.getMessage());
//        }
//    }
//
//    @Override
//    public NodeResponse setLinkScheduledFor(LinkID originalId, LocalDateTime manualScheduledFor) {
//        TaskLink link = this.entityQueryService.getLink(originalId);
//        link.manualScheduledFor(manualScheduledFor);
//        return new NodeResponse(true, link, this.OK);
//    }
//
//    @Override
//    public TagResponse createTag(Tag tag) {
//        try {
//            logger.debug("Creating tag " + tag);
//
//            TagEntity tagEntity = dataContext.mergeTag(tag);
//
//            return new TagResponse(true, tagEntity, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new TagResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public TagResponse findTagOrElseCreate(String name) {
//        try {
//            logger.debug("Finding or creating tag named " + name);
//
//            Optional<TagEntity> tagOptional = entityQueryService.findTag(name);
//
//            if (tagOptional.isEmpty()) {
//                return this.createTag(new Tag(null, name));
//            }
//
//            TagEntity tagEntity = tagOptional.get();
//
//            return new TagResponse(true, tagEntity, this.OK);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new TagResponse(false, null, e.getMessage());
//        }
//    }
//
//    @Override
//    public NodesResponse deepCopyTaskNode(TaskNodeDTO original, TaskFilter filter) {
//        return this.deepCopyTaskNode(original, filter, "");
//    }
//
//    @Override
//    public SyncResponse deepCopyTaskNode(TaskNodeDTO original, TaskFilter filter, String suffix) {
//        try {
//            return tryDeepCopy(original, filter, suffix);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new NodesResponse(false, null, e.getMessage());
//        }
//    }
//
//    private Task makeCopyOfTask(TaskEntity task, String suffix) {
//        suffix = suffix.isBlank()
//                ? " (copy)"
//                : suffix;
//        Task copy = DataContext.toDTO(task).copyWithoutID();
//        return copy.name(copy.name() + suffix);
//    }
//
//    @Transactional(rollbackFor = Exception.class)
//    private NodesResponse tryDeepCopy(TaskNodeDTO original, TaskFilter filter, String suffix) {
//        logger.debug("Deep copy from " + original + " with filter " + filter);
//
//        TaskEntity rootTaskEntity = entityQueryService.getTask(original.childId());
//        Stream<TaskLink> descendants = entityQueryService.findDescendantLinks(original.childId(), filter);
//
//        Task rootTaskCopy = makeCopyOfTask(rootTaskEntity, suffix);
//        TaskEntity rootTaskEntityCopy = entityQueryService.getTask(this.createTask(rootTaskCopy).task().changes());
//
//        NodeResponse nodeResponse = this.insertNode(original.childId(ID.of(rootTaskEntityCopy)));
//
//        List<TaskNode> results = new LinkedList<>();
//        results.add(nodeResponse.node());
//
//        descendants.forEach(link -> {
//            String finalSuffix = suffix.isBlank()
//                    ? " (" + link.parent().name() + ")"
//                    : suffix;
//
//            Task task = makeCopyOfTask(link.child(), finalSuffix);
//            TaskEntity taskEntity = entityQueryService.getTask(this.createTask(task).task().changes());
//            TaskNode node = this.insertNode(
//                    new TaskNodeDTO(
//                            ID.of(link.parent()),
//                            ID.of(taskEntity),
//                            DataContext.toDTO(link)))
//                    .node();
//
//            results.add(node);
//        });
//
//        return new NodesResponse(true, results.stream(), K.OK);
//    }
//
//    @Override
//    public void recalculateTimeEstimates() {
//        timeEstimateRepository.findAll()
//                .forEach(estimate -> estimate.totalDuration(Duration.ZERO));
//
//        entityQueryService.findTasks(null)
//                .forEach(task -> {
//                    Duration sum = entityQueryService.findDescendantLinks(ID.of(task), null)
//                            .map(link ->
//                                    link.child().project()
//                                            ? link.projectDuration()
//                                            : link.child().duration()
//                            )
//                            .reduce(task.duration(),
//                                    Duration::plus);
//                    task.timeEstimates().get(0).totalDuration(sum);
//        });
//    }
//}
