package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeDTOData;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeTemplateData;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.*;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.facade.response.Request;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class ChangeProcessor {
    @Autowired private DataContext dataContext;
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private NetDurationService netDurationService;
    @Autowired private RoutineService routineService;

    @Setter
    private Consumer<Set<TaskLink>> updateSyncManagerDurations;

    public synchronized Pair<String, MultiValueMap<ChangeID, PersistedDataDO<?>>> process(Request request) {
        MultiValueMap<ChangeID, PersistedDataDO<?>> dataResults = new LinkedMultiValueMap<>();

        Deque<String> messages = new LinkedList<>();
        BiFunction<String, PersistedDataDO<?>, String> messageSupplier = (str, data) ->
                str + " " + data.typeName().toLowerCase() + " \"" + data.name() + "\"";

        BiFunction<String, String, String> biMessageSupplier = (str, body) ->
                str + " " + body;

        Set<TaskLink> durationUpdates = new HashSet<>();

        for (Change change : request.changes()) {
            String prefix;
            if (change instanceof PersistChange) {
                prefix = "Created ";
                Data data = ((PersistChange<?>) change).data();
                if (data instanceof Task task) {
                    if (task.id() != null)
                        throw new IllegalArgumentException("Cannot persist task with ID: " + task.id());
                    // Currently, we never persist a task without a task node as well
                    // log.debug("Persisting task: {}", task);
                    Task result = dataContext.toDO(dataContext.mergeTask(task));
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, task));
                } else if (data instanceof TaskNodeDTO taskNodeDTO) {
                    log.debug("Persisting task node: {}", taskNodeDTO);
                    TaskLink resultLink = dataContext.mergeNode(taskNodeDTO);
                    updateDuration(durationUpdates, resultLink);
                    TaskNode result = dataContext.toDO(resultLink);
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, result));
                } else if (data instanceof Tag tag) {
                    if (tag.id() != null)
                        throw new IllegalArgumentException("Cannot persist task with ID: " + tag.id());
                    log.debug("Persisting tag: {}", tag);
                    Tag result = dataContext.toDO(dataContext.mergeTag(tag));
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, result));
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + data);
                }
            } else if (change instanceof MergeChange) {
                prefix = "Updated ";
                Data data = ((MergeChange<?>) change).data();
                if (data instanceof Task task) {
                    if (task.id() == null) throw new IllegalArgumentException("Cannot merge task without ID: " + task);
                    log.debug("Merging task: {}", task);
                    TaskEntity resultEntity = dataContext.mergeTask(task);
                    updateDuration(durationUpdates, resultEntity);
                    Task result = dataContext.toDO(resultEntity);
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, result));
                } else if (data instanceof TaskNode taskNode) {
                    log.debug("Merging node: {}", taskNode);
                    TaskLink resultLink = dataContext.mergeNode(taskNode);
                    TaskNode result = dataContext.toDO(resultLink);
                    updateDuration(durationUpdates, resultLink);
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, result));
                } else if (data instanceof Tag tag) {
                    if (tag.id() != null)
                        throw new IllegalArgumentException("Cannot merge tag without ID: " + tag.id());
                    log.debug("Merging tag: {}", tag);
                    Tag result = dataContext.toDO(dataContext.mergeTag(tag));
                    dataResults.add(change.id(), result);
                    messages.add(messageSupplier.apply(prefix, result));
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + data);
                }
            } else if (change instanceof DeleteChange) {
                prefix = "Deleted ";
                ID id = ((DeleteChange<?>) change).data();
                if (id instanceof TaskID taskId) {
                    log.debug("Deleting task: {}", taskId);
                    throw new NotImplementedException();
                } else if (id instanceof LinkID linkId) {
                    log.debug("Deleting link: {}", linkId);
                    TaskLink target = entityQueryService.getLink(linkId);
                    updateDuration(durationUpdates, target);
                    messages.add(biMessageSupplier.apply(prefix, "task node \"" + target.child().name() + "\""));
                    dataContext.deleteLink(target);
                } else if (id instanceof TagID tagId) {
                    log.debug("Deleting tag: {}", tagId);
                    throw new NotImplementedException();
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type: " + id);
                }
            } else if (change instanceof InsertChange<?> insertChange) {
                prefix = "Added ";
                TaskNodeDTO taskNodeDTO = insertChange.nodeDTO();

                if (insertChange instanceof ReferencedInsertChange referencedInsertChange) {
                    Task task = (Task) dataResults.getFirst(referencedInsertChange.changeTaskReference());
                    taskNodeDTO = referencedInsertChange.nodeDTO().childId(task.id());
                    log.debug("Persisting task node with task from referenced change: {}, {}", task, taskNodeDTO);
                }

                if (insertChange instanceof InsertIntoChange insertIntoChange) {
                    for (Map.Entry<TaskID, List<InsertLocation>> entry : insertIntoChange.locations().entrySet()) {
                        TaskID taskId = entry.getKey();
                        List<InsertLocation> insertLocations = entry.getValue();

                        TaskEntity reference = (taskId != null) ? entityQueryService.getTask(taskId) : null;
                        for (InsertLocation location : insertLocations) {
                            log.debug("Inserting task node dto {} \n into {} {}", insertIntoChange.nodeDTO(), location.name(), reference);
                            setPositionBasedOnLocation(taskNodeDTO, reference, location);
                            TaskLink resultLink = dataContext.mergeNode(insertIntoChange.nodeDTO());
                            updateDuration(durationUpdates, resultLink);
                            TaskNode result = dataContext.toDO(resultLink);
                            dataResults.add(insertIntoChange.id(), result);
                            messages.add(messageSupplier.apply(prefix, result));
                        }
                    }
                } else if (insertChange instanceof InsertAtChange insertAtChange) {
                    for (Map.Entry<LinkID, List<InsertLocation>> entry : insertAtChange.locations().entrySet()) {
                        LinkID linkId = entry.getKey();
                        List<InsertLocation> insertLocations = entry.getValue();
                        TaskLink reference = (linkId == null) ? null : entityQueryService.getLink(linkId);

                        for (InsertLocation location : insertLocations) {
                            log.debug("Inserting task node dto {} \n to {} {}", insertAtChange.nodeDTO(), location.name(), reference);
                            setPositionBasedOnLocation(taskNodeDTO, reference, location);
                            TaskLink resultLink = dataContext.mergeNode(insertAtChange.nodeDTO());
                            updateDuration(durationUpdates, resultLink);
                            TaskNode result = dataContext.toDO(resultLink);
                            dataResults.add(insertAtChange.id(), result);
                            messages.add(messageSupplier.apply(prefix, result));
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type");
                }
            } else if (change instanceof MoveChange moveChange) {
                prefix = "Moved ";
                TaskLink original = entityQueryService.getLink(moveChange.originalId());
                TaskNodeDTO dto = new TaskNodeDTO(original);
                for (Map.Entry<LinkID, List<InsertLocation>> entry : moveChange.locations().entrySet()) {
                    LinkID referenceId = entry.getKey();
                    List<InsertLocation> insertLocations = entry.getValue();

                    TaskLink reference = (referenceId == null) ? null : entityQueryService.getLink(referenceId);
                    for (InsertLocation location : insertLocations) {
                        log.debug("Moving task node {} \n to {} {}", original, location.name(), reference);
                        setPositionBasedOnLocation(dto, reference, location);
                        TaskLink resultLink = dataContext.mergeNode(dto);
                        resultLink.scheduledFor(original.scheduledFor());
                        updateDuration(durationUpdates, resultLink);
                        TaskNode result = dataContext.toDO(resultLink);
                        dataResults.add(moveChange.id(), result);
                        messages.add(messageSupplier.apply(prefix, result));
                        updateDuration(durationUpdates, resultLink);
                        dataContext.deleteLink(original);
                    }
                }
            } else if (change instanceof MultiMergeChange<?, ?> multiMerge) {
                Data template = multiMerge.template();

                if (template instanceof TaskDTO taskTemplate) {
                    log.debug("Merging task template: {}", taskTemplate);
                    for (ID taskId : multiMerge.ids()) {
                        TaskEntity resultEntity = dataContext.mergeTaskTemplate(
                                (TaskID) taskId,
                                taskTemplate);
                        updateDuration(durationUpdates, resultEntity);
                        dataResults.add(multiMerge.id(), dataContext.toDO(resultEntity));
                    }
                    messages.add("Merged " + multiMerge.ids().size() + " task changes.");
                } else if (template instanceof TaskNodeTemplateData<?> nodeTemplate) {
                    log.debug("Merging node template: {}", nodeTemplate);
                    for (ID linkId : multiMerge.ids()) {
                        TaskLink resultLink = dataContext.mergeNodeTemplate(
                                (LinkID) linkId,
                                nodeTemplate);
                        updateDuration(durationUpdates, resultLink);
                        dataResults.add(multiMerge.id(), dataContext.toDO(resultLink));
                    }
                    messages.add("Merged " + multiMerge.ids().size() + " task node changes.");
                } else {
                    throw new IllegalArgumentException("Unexpected changeRelevantDataMap type");
                }
            } else if (change instanceof CopyChange copyChange) {
                switch (copyChange.copyType()) {
                    case SHALLOW -> {
                        prefix = "Shallow copied ";
                        throw new NotImplementedException();
                    }
                    case DEEP -> {
                        prefix = "Deep copied ";
                        log.debug("Deep copying task node: {}", copyChange);
                        TaskLink original = entityQueryService.getLink(copyChange.originalId());
                        copyChange.locations().forEach((referenceId, insertLocations) -> {
                            TaskLink reference = entityQueryService.getLink(referenceId);
                            insertLocations.forEach(location -> {
                                TaskNodeDTO originalDTO = original.toDTO();
                                setPositionBasedOnLocation(originalDTO, reference, location);

                                TaskLink newRootLink = this.tryDeepCopy(originalDTO, copyChange.taskFilter(), copyChange.suffix());
                                updateDuration(durationUpdates, newRootLink);
                                dataResults.add(copyChange.id(), dataContext.toDO(newRootLink));
                            });
                        });
                        messages.add(biMessageSupplier.apply(prefix, original.typeName() + " \"" + original.child().name() + "\""));
                    }
                }
            } else if (change instanceof OverrideScheduledForChange overrideScheduledForChange) {
                TaskLink link = entityQueryService.getLink(overrideScheduledForChange.linkId());
                updateDuration(durationUpdates, link);
                link.scheduledFor(overrideScheduledForChange.manualScheduledFor());
                TaskNode result = dataContext.toDO(link);
                dataResults.add(overrideScheduledForChange.id(), result);
                messages.add(messageSupplier.apply("Set scheduled time for", result));
            } else if (change instanceof InsertRoutineStepChange insertStepChange) {
                TaskEntity task = dataContext.mergeTask(insertStepChange.task());
                RoutineEntity routine = entityQueryService.getRoutine(insertStepChange.routineId());
                RoutineStepEntity currentStep = routine.currentStep();
                int currentStepPosition = currentStep.position();
                RoutineStepEntity newStep = new RoutineStepEntity(task);
                newStep.routine(routine);
                newStep.position(currentStepPosition);
                currentStep.parentStep().children().forEach(step -> {
                    if (step.position() >= currentStepPosition) {
                        step.position(step.position() + 1);
                    }
                });

                currentStep.parentStep().children().add(currentStepPosition, newStep);
            } else {
                throw new UnsupportedOperationException("Unexpected change type: " + change.getClass());
            }
        }

        String message;
        if (messages.size() > 5) {
            message = "Synced " + messages.size() + " changes";
        } else {
            StringJoiner joiner = new StringJoiner("\r\n ");
            messages.forEach(joiner::add);
            message = joiner.toString();
        }

        routineService.notifyChanges(durationUpdates);
        netDurationService.clearLinks(durationUpdates);
        updateSyncManagerDurations.accept(durationUpdates);
        return Pair.of(message, dataResults);
    }

    private void setPositionBasedOnLocation(TaskNodeDTOData<?> nodeDTO, Data reference, InsertLocation location) {
        if (reference == null) {
            nodeDTO.parentId(null);
            switch (location) {
                case FIRST -> nodeDTO.position(0);
                case CHILD, LAST -> nodeDTO.position(null);
                case BEFORE, AFTER -> throw new IllegalArgumentException("Must provide a TaskLink as a reference for inserting BEFORE, AFTER");
            }
        } else if (reference instanceof TaskEntity referenceTask) {
            nodeDTO.parentId(ID.of(referenceTask));
            switch (location) {
                case FIRST -> nodeDTO.position(0);
                case CHILD, LAST -> nodeDTO.position(null);
                case BEFORE, AFTER -> throw new IllegalArgumentException("Must provide a TaskLink as a reference for inserting BEFORE, AFTER");
            }
        } else if (reference instanceof TaskLink referenceLink) {
            switch (location) {
                case FIRST -> nodeDTO
                        .parentId(ID.of(referenceLink.child()))
                        .position(0);
                case CHILD, LAST -> nodeDTO
                        .parentId(ID.of(referenceLink.child()))
                        .position(null);
                case BEFORE -> nodeDTO
                        .parentId(ID.of(referenceLink.parent()))
                        .position(referenceLink.position());
                case AFTER -> nodeDTO
                        .parentId(ID.of(referenceLink.parent()))
                        .position(referenceLink.position() + 1);
            }
        } else {
            throw new IllegalArgumentException("Unexpected reference type: " + reference.getClass());
        }
    }

    private void updateDuration(Set<TaskLink> durationUpdates, TaskLink link) {
        durationUpdates.add(link);
        durationUpdates.addAll(entityQueryService.findAncestorLinks(
                ID.of(link.child()), null)
                .collect(Collectors.toSet()));
        if (link.child().project() && !link.projectDurationLimit().isZero()) {
            durationUpdates.addAll(entityQueryService.findChildLinks(
                    ID.of(link.child()), null)
                    .collect(Collectors.toSet()));
        }
    }

    private void updateDuration(Set<TaskLink> durationUpdates, TaskEntity task) {
        for (TaskLink parent : task.parentLinks()) {
            durationUpdates.addAll(entityQueryService.findAncestorLinks(
                    ID.of(parent.child()), null)
                    .collect(Collectors.toSet()));
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
        Task copy = dataContext.toDO(task).copyWithoutID();
        return copy.name(copy.name() + suffix);
    }

    private TaskLink tryDeepCopy(TaskNodeDTO original, TaskNodeTreeFilter filter, String suffix) {
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

            dataContext.mergeNode(dataContext.toDO(link).toDTO()
                    .parentId(copiedParentId)
                    .childId(copiedTaskId));

            copiedTasks.put(ID.of(link.child()), copiedTaskId);
        });

        return rootLink;
    }
}
