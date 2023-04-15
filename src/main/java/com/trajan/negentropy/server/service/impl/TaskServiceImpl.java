package com.trajan.negentropy.server.service.impl;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskInfo_;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.FilteredTaskInfoRepository;
import com.trajan.negentropy.server.repository.FilteredTaskNodeRepository;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import com.trajan.negentropy.server.service.TaskService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("taskService")
@Transactional
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private static final String ROOT = "_ROOT";
    private final FilteredTaskInfoRepository filteredTaskInfoRepository;
    private final FilteredTaskNodeRepository filteredTaskNodeRepository;

    private Long rootId;

    public TaskServiceImpl(FilteredTaskInfoRepository filteredTaskInfoRepository, FilteredTaskNodeRepository filteredTaskNodeRepository) {
        this.filteredTaskInfoRepository = filteredTaskInfoRepository;
        this.filteredTaskNodeRepository = filteredTaskNodeRepository;
    }

    public TaskInfo saveTaskInfo(TaskInfo taskInfo) {
        TaskInfo ret = filteredTaskInfoRepository.getTaskInfoRepository().save(taskInfo);
        logger.debug("Saved taskInfo: " + ret.getTitle());
        return ret;
    }

    public TaskInfo getTaskInfoById(Long id) {
        return filteredTaskInfoRepository.getTaskInfoRepository().getReferenceById(id);
    }

    public List<TaskInfo> findTaskInfos(List<Filter> filters) {
        filters = new ArrayList<>(filters);
        filters.add(Filter.builder()
                .field(TaskInfo_.TITLE)
                .operator(QueryOperator.NOT_EQ)
                .value(ROOT)
                .build());
         return filteredTaskInfoRepository.findByFilters(filters);
    }

    public void deleteTaskInfo(TaskInfo taskInfo) {
        if (taskInfo.getTitle().equals(ROOT)) {
            throw new IllegalArgumentException("Cannot delete root TaskInfo.");
        }
        for (TaskNode child : taskInfo.getChildren()) {
            deleteTaskNode(child);
        }
        for (TaskNode node : taskInfo.getNodes()) {
            deleteTaskNode(node);
        }
        filteredTaskInfoRepository.getTaskInfoRepository().delete(taskInfo);
    }

    public TaskInfo getRootTaskInfo() {
        TaskInfo rootInfo = null;
        if (rootId != null) {
            rootInfo = getTaskInfoById(rootId);
        }
        if (rootInfo == null || rootId == null) {
            Filter rootFilter = Filter.builder()
                    .field(TaskInfo_.TITLE)
                    .operator(QueryOperator.EQUALS)
                    .value(ROOT)
                    .build();
            List<TaskInfo> result = filteredTaskInfoRepository.findByFilters(List.of(rootFilter));
            if (result.isEmpty()) {
                logger.warn("No root relationship found! Creating one...");
                TaskInfo rootTaskInfo = new TaskInfo();
                rootTaskInfo.setTitle(ROOT);
                return filteredTaskInfoRepository.getTaskInfoRepository().save(rootTaskInfo);
            } else if (result.size() > 1) {
                logger.error("MULTIPLE ROOT RELATIONSHIPS FOUND! Database malformed. Using first found...");
            }
            rootInfo = result.get(0);
        }
        return rootInfo;
    }

    public TaskNode saveTaskNode(TaskNode taskNode) throws IllegalArgumentException {
        if (taskNode.getParent() == null) {
            // We are adding this as a sibling of the root node
            taskNode.setParent(getRootTaskInfo());
        }
        TaskNode prevNode = taskNode.getPrev();
        TaskNode nextNode = taskNode.getNext();

        // Case 1: Only the 'next' is specified
        if (nextNode != null && prevNode == null) {
            prevNode = nextNode.getPrev();
            if (prevNode != null) {
                taskNode.setPrev(prevNode);
                prevNode.setNext(taskNode);
            }
        }
        // Case 2: Both 'next' and 'prev' are specified
        else if (nextNode != null) { // prevNode != null
            prevNode.setNext(taskNode);
            nextNode.setPrev(taskNode);
        }
        // Case 3: Neither 'next' nor 'prev' objects are specified
        else if (prevNode == null) { // nextNode == null
            // Check if parent has any children
            if (!taskNode.getParent().getChildren().isEmpty()) {
                throw new IllegalArgumentException(
                        "Parent has existing children; new TaskNode should specify at least 'next'.");
            }
        }
        // Invalid combination of arguments
        else {
            throw new IllegalArgumentException("Invalid combination of arguments");
        }

        List<TaskNode> changes = new LinkedList<>(Arrays.asList(
                taskNode,
                prevNode,
                nextNode
        ));

        changes.removeAll(Collections.singleton(null));
        // Verify all nodes have the same parent
        for(TaskNode node : changes) {
            if (node.getParent() != taskNode.getParent()) {
                throw new IllegalArgumentException("Failed when saving TaskNode: associated nodes don't share the same parent..");
            }
        }

        return filteredTaskNodeRepository.getTaskNodeRepository().saveAll(changes).get(0);
    }

    public TaskNode getTaskNodeById(Long id) {
        return filteredTaskNodeRepository.getTaskNodeRepository().getReferenceById(id);
    }

    public List<TaskNode> findTaskNodes(List<Filter> filters) {
        return filteredTaskNodeRepository.findByFilters(filters);
    }

    public void deleteTaskNode(TaskNode taskNode) throws IllegalArgumentException {
        TaskNode prevNode = taskNode.getPrev();
        TaskNode nextNode = taskNode.getNext();

        if (prevNode == null && nextNode == null) {
            // Nothing to update, return
            return;
        }

        // Update prevNode's next or nextNode's prev accordingly
        if (prevNode != null && nextNode == null) {
            prevNode.setNext(null);
        } else if (prevNode == null && nextNode != null) {
            nextNode.setPrev(null);
        } else {
            prevNode.setNext(nextNode);
            nextNode.setPrev(prevNode);
        }

        List<TaskNode> changes = new LinkedList<>(Arrays.asList(prevNode, nextNode));

        // Verify all nodes have the same parent
        for (TaskNode node : changes) {
            if (node != null) {
                if (node.getParent() != taskNode.getParent()) {
                    throw new IllegalArgumentException("Failed when deleting TaskNode: associated nodes don't share the same parent.");
                }
            }
        }

        // Delete the taskNode and add it to the list of changes
        filteredTaskNodeRepository.getTaskNodeRepository().delete(taskNode);
        changes.add(taskNode);

        // Save all changes
        filteredTaskNodeRepository.getTaskNodeRepository().saveAll(changes);
    }

}