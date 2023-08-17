package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskData.TaskTemplateData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.sync.SyncManager;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Slf4j
public class ChangeService {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private DataContext dataContext;
    @Autowired private SyncManager syncManager;

    public synchronized DataMapResponse execute(Request request) {
        log.debug("Processing change request with sync id {}, change count {}",
                request.syncId(),
                request.changes().size());
        boolean sync = request.syncId() != null;
        try {
            MultiValueMap<Integer, PersistedDataDO<?>> dataResults = process(request);

            log.trace("Data results: " + dataResults);

            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : null;

            return new DataMapResponse(dataResults, aggregateSyncRecord);
        } catch (Exception e) {
            e.printStackTrace();
            SyncRecord aggregateSyncRecord = sync
                    ? syncManager.aggregatedSyncRecord(request.syncId())
                    : null;

            return new DataMapResponse(e.getMessage(), aggregateSyncRecord);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private MultiValueMap<Integer, PersistedDataDO<?>> process(Request request) {
        MultiValueMap<Integer, PersistedDataDO<?>> dataResults = new LinkedMultiValueMap<>();
        Map<Task, TaskEntity> newTasks = new HashMap<>();

        for (Change change : request.changes()) {
            if (change instanceof PersistChange) {
                Data data = ((PersistChange<?>) change).data();
                if (data instanceof Task task) {
                    if (task.id() != null)
                        throw new IllegalArgumentException("Cannot persist task with ID: " + task.id());
                    log.debug("Persisting task: {}", task);
                    TaskEntity taskEntity = dataContext.mergeTask(task);
                    dataResults.add(change.id(), DataContext.toDO(taskEntity));
                    newTasks.put(task, taskEntity);
                } else if (data instanceof TaskNodeDTO taskNodeDTO) {
                    log.debug("Persisting task node: {}", taskNodeDTO);
                    dataResults.add(((PersistChange<?>) change).id(), DataContext.toDO(dataContext.mergeNode(taskNodeDTO)));
                } else if (data instanceof Tag tag) {
                    if (tag.id() != null)
                        throw new IllegalArgumentException("Cannot persist task with ID: " + tag.id());
                    log.debug("Persisting tag: {}", tag);
                    dataResults.add(change.id(), DataContext.toDO(dataContext.mergeTag(tag)));
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + data);
                }
            } else if (change instanceof MergeChange) {
                Data data = ((MergeChange<?>) change).data();
                if (data instanceof Task task) {
                    if (task.id() == null) throw new IllegalArgumentException("Cannot merge task without ID: " + task);
                    log.debug("Merging task: {}", task);
                    dataResults.add(change.id(), DataContext.toDO(dataContext.mergeTask(task)));
                } else if (data instanceof TaskNode taskNode) {
                    log.debug("Merging node: {}", taskNode);
                    dataResults.add(change.id(), DataContext.toDO(dataContext.mergeNode(taskNode)));
                } else if (data instanceof Tag tag) {
                    if (tag.id() != null)
                        throw new IllegalArgumentException("Cannot merge tag without ID: " + tag.id());
                    log.debug("Merging tag: {}", tag);
                    dataResults.add(change.id(), DataContext.toDO(dataContext.mergeTag(tag)));
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + data);
                }
            } else if (change instanceof DeleteChange) {
                ID id = ((DeleteChange<?>) change).data();
                if (id instanceof TaskID taskId) {
                    log.debug("Deleting task: {}", taskId);
                    throw new NotImplementedException();
                } else if (id instanceof LinkID linkId) {
                    log.debug("Deleting link: {}", linkId);
                    dataContext.deleteLink(entityQueryService.getLink(linkId));
                } else if (id instanceof TagID tagId) {
                    log.debug("Deleting tag: {}", tagId);
                    throw new NotImplementedException();
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + id);
                }
            } else if (change instanceof InsertChange insertChange) {
                TaskNodeDTO taskNodeDTO = insertChange.nodeDTO();

                if (insertChange instanceof ReferencedInsertChange referencedInsertChange) {
                    Task task = (Task) dataResults.getFirst(referencedInsertChange.changeTaskReference());
                    taskNodeDTO = referencedInsertChange.nodeDTO().childId(task.id());
                    log.debug("Persisting task node with task from referenced change: {}, {}", task, taskNodeDTO);
                }

                TaskNodeDTO finalDTO = taskNodeDTO;
                insertChange.locations().forEach((linkId, insertLocations) -> {
                    TaskLink reference = linkId == null ? null : entityQueryService.getLink(linkId);
                    insertLocations.forEach(location -> {
                        log.debug("Inserting task node dto {} \n to {} {}", insertChange.nodeDTO(), location.name(), reference);
                        setPositionBasedOnLocation(finalDTO, reference, location);
                        dataResults.add(insertChange.id(), DataContext.toDO(dataContext.mergeNode(finalDTO)));
                    });
                });
            } else if (change instanceof InsertIntoChange insertIntoChange) {
                insertIntoChange.locations().forEach((taskId, insertLocations) -> {
                    TaskEntity reference = (taskId != null) ? entityQueryService.getTask(taskId) : null;
                    insertLocations.forEach(location -> {
                        log.debug("Inserting task node dto {} \n into {} {}", insertIntoChange.nodeDTO(), location.name(), reference);
                        setPositionBasedOnLocation(insertIntoChange.nodeDTO(), reference, location);
                        dataResults.add(insertIntoChange.id(), DataContext.toDO(dataContext.mergeNode(insertIntoChange.nodeDTO())));
                    });
                });
            } else if (change instanceof MoveChange moveChange) {
                TaskLink original = entityQueryService.getLink(moveChange.originalId());
                TaskNodeDTO dto = new TaskNodeDTO(original);
                moveChange.locations().forEach((referenceId, insertLocations) -> {
                    TaskLink reference = (referenceId == null) ? null : entityQueryService.getLink(referenceId);
                    insertLocations.forEach(location -> {
                        log.debug("Moving task node {} \n to {} {}", original, location.name(), reference);
                        setPositionBasedOnLocation(dto, reference, location);
                        dataResults.add(moveChange.id(), DataContext.toDO(dataContext.mergeNode(dto)));
                        dataContext.deleteLink(original);
                    });
                });
            } else if (change instanceof MultiMergeChange<?, ?> multiMerge) {
                Data template = multiMerge.template();

                if (template instanceof TaskTemplateData<?, ?> taskTemplate) {
                    log.debug("Merging task template: {}", taskTemplate);
                    for (ID taskId : multiMerge.ids()) {
                        dataResults.add(multiMerge.id(), DataContext.toDO(
                                dataContext.mergeTaskTemplate(
                                        (TaskID) taskId,
                                        (TaskTemplateData<Task, Tag>) taskTemplate)));
                    }
                } else if (template instanceof TaskNodeTemplateData<?> nodeTemplate) {
                    log.debug("Merging node template: {}", nodeTemplate);
                    for (ID linkId : multiMerge.ids()) {
                        dataResults.add(multiMerge.id(), DataContext.toDO(
                                dataContext.mergeNodeTemplate(
                                        (LinkID) linkId,
                                        nodeTemplate)));
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type");
                }
            } else if (change instanceof CopyChange copyChange) {
                switch (copyChange.copyType()) {
                    case SHALLOW -> {
                        throw new NotImplementedException();
                    }
                    case DEEP -> {
                        log.debug("Deep copying task node: {}", copyChange);
                        TaskLink original = entityQueryService.getLink(copyChange.originalId());
                        copyChange.locations().forEach((referenceId, insertLocations) -> {
                            TaskLink reference = entityQueryService.getLink(referenceId);
                            insertLocations.forEach(location -> {
                                TaskNodeDTO originalDTO = original.toDTO();
                                setPositionBasedOnLocation(originalDTO, reference, location);

                                TaskLink newRootLink = this.tryDeepCopy(originalDTO, copyChange.taskFilter(), copyChange.suffix());

                                dataResults.add(copyChange.id(), DataContext.toDO(newRootLink));
                            });
                        });
                    }
                }
            } else if (change instanceof OverrideScheduledForChange overrideScheduledForChange) {
                TaskLink link = entityQueryService.getLink(overrideScheduledForChange.linkId());
                link.scheduledFor(overrideScheduledForChange.manualScheduledFor());
                dataResults.add(overrideScheduledForChange.id(), DataContext.toDO(link));
            } else {
                throw new UnsupportedOperationException("Unexpected change type: " + change.getClass());
            }
        }







//                    case ReferencedInsertChange referencedPersist -> {
//                        Task task = (Task) dataResults.getFirst(referencedPersist.changeTaskReference());
//                        TaskNodeDTO taskNodeDTO = referencedPersist.nodeDTO()
//                                .childId(task.id());
//
//                        referencedPersist.locations().forEach((linkId, insertLocations) -> {
//                            TaskLink reference = entityQueryService.getLink(linkId);
//                            insertLocations.forEach(location -> {
//                                setPositionBasedOnLocation(referencedPersist.nodeDTO(), reference, location);
//
//                                dataResults.add(referencedPersist.id(), DataContext.toDO(
//                                        dataContext.mergeNode(referencedPersist.nodeDTO())));
//                            });
//                        });
//
//                        TaskNodeDTO taskNodeDTO = referencedPersist.nodeDTO()
//                                .childId(task.id());
//                        log.debug("Persisting task node with task from referenced change: {}, {}", task, taskNodeDTO);
//                        dataResults.add(referencedPersist.id(), DataContext.toDO(dataContext.mergeNode(taskNodeDTO)));
//                    }

        return dataResults;
    }

    private void setPositionBasedOnLocation(TaskNodeDTOData<?> nodeDTO, TaskEntity referenceTask, InsertLocation location) {
        switch (location) {
            case FIRST -> {
                nodeDTO
                        .parentId(referenceTask == null
                                ? null
                                : ID.of(referenceTask))
                        .position(0);
            }
            case CHILD, LAST -> {
                nodeDTO
                        .parentId(referenceTask == null
                                ? null
                                : ID.of(referenceTask))
                        .position(null);
            }
            case BEFORE, AFTER -> {
                throw new IllegalArgumentException("Must provide a TaskLink as a reference for inserting BEFORE, AFTER");
            }
        }
    }

    private void setPositionBasedOnLocation(TaskNodeDTOData<?> nodeDTO, TaskLink referenceLink, InsertLocation location) {
        switch (location) {
            case FIRST -> {
                nodeDTO
                        .parentId(referenceLink == null
                                ? null
                                : ID.of(referenceLink.child()))
                        .position(0);
            }
            case CHILD, LAST -> {
                nodeDTO
                        .parentId(referenceLink == null
                                ? null
                                : ID.of(referenceLink.child()))
                        .position(null);
            }
            case BEFORE -> {
                if (referenceLink == null) {
                    throw new IllegalArgumentException("Link ID cannot be null" +
                            " with insert location BEFORE");
                } else {
                    nodeDTO
                            .parentId(ID.of(referenceLink.parent()))
                            .position(referenceLink.position());
                }
            }
            case AFTER -> {
                if (referenceLink == null) {
                    throw new IllegalArgumentException("Link ID cannot be null" +
                            " with insert location AFTER");
                } else {
                    nodeDTO
                            .parentId(ID.of(referenceLink.parent()))
                            .position(referenceLink.position()+1);
                }
            }
        }
    }

//
//    public DataResponse<MultiValueMap<Integer, ID>> deepCopyTaskNode(BiRequest<TaskNodeDTO, TaskFilter> request) {
//        // TaskNodeDTO original, TaskFilter filter
//        return this.deepCopyTaskNode(new TriRequest<>(request.syncId(), request.changeRelevantDataMap(), request.data2(), ""));
//    }
//
//    public DataResponse<MultiValueMap<Integer, ID>> deepCopyTaskNode(TriRequest<TaskNodeDTO, TaskFilter, String> request) {
//        // TaskNodeDTO original, TaskFilter filter, String suffix)
//         return this.process("Deep copy from " + request.changeRelevantDataMap() + " with filter " + request.data2(), request,
//                () -> {
//             MultiValueMap<Integer, ID> results = new LinkedMultiValueMap<>();
//
//                }
//                tryDeepCopy(request.changeRelevantDataMap(), request.data2(), request.data3()));
//    }

    private Task makeCopyOfTask(TaskEntity task, String suffix) {
        suffix = suffix.isBlank()
                ? " (copy)"
                : suffix;
        Task copy = DataContext.toDO(task).copyWithoutID();
        return copy.name(copy.name() + suffix);
    }

    private TaskLink tryDeepCopy(TaskNodeDTO original, TaskFilter filter, String suffix) {
        log.debug("Deep copy from " + original + " with filter " + filter);

        TaskEntity rootTaskEntity = entityQueryService.getTask(original.childId());
        Stream<TaskLink> descendants = entityQueryService.findDescendantLinks(original.childId(), filter);

        Task rootTaskCopy = makeCopyOfTask(rootTaskEntity, suffix);
        TaskID copiedRootTaskId = ID.of(dataContext.mergeTask(rootTaskCopy));
        TaskLink rootLink = dataContext.mergeNode(original
                .parentId(ID.of(rootTaskEntity))
                .childId(copiedRootTaskId));

        Map<TaskID, TaskID> copiedTasks = new HashMap<>();
        copiedTasks.put(ID.of(rootTaskEntity), copiedRootTaskId);

        descendants.forEach(link -> {
            String finalSuffix = suffix.isBlank()
                    ? " (" + link.parent().name() + ")"
                    : suffix;

            Task task = makeCopyOfTask(link.child(), finalSuffix);
            TaskID copiedTaskId = ID.of(dataContext.mergeTask(task));
            TaskID copiedParentId = copiedTasks.get(ID.of(link.parent()));

            dataContext.mergeNode(DataContext.toDO(link).toDTO()
                    .parentId(copiedParentId)
                    .childId(copiedTaskId));

            copiedTasks.put(ID.of(link.child()), copiedTaskId);
        });

        return rootLink;
    }

//    public DataResponse<TaskNode> setLinkScheduledFor(BiRequest<LinkID, LocalDateTime> request) {
//        return this.processWithResult("Set link scheduled for " + request.data2(), request, () -> {
//            TaskLink link = this.entityQueryService.getLink(request.changeRelevantDataMap());
//            link.scheduledFor(request.data2());
//            return DataContext.toDO(link);
//        });
//    }
}