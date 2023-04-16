package com.trajan.negentropy.server.service.impl;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.TaskNode_;
import com.trajan.negentropy.server.repository.TaskNodeRepository;
import com.trajan.negentropy.server.repository.TaskRepository;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.QueryOperator;
import com.trajan.negentropy.server.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("taskService")
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final TaskRepository taskRepository;
    private final TaskNodeRepository nodeRepository;

    public TaskServiceImpl(TaskRepository taskRepository, TaskNodeRepository nodeRepository) {
        this.taskRepository = taskRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    @Transactional
    public Task createTask(Task task) {
        if (task.getId() == null) {
            return taskRepository.save(task);
        } else {
            throw new IllegalArgumentException("Attempted to create a new Task object with existing ID.");
        }
    }

    @Override
    @Transactional
    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }

    @Override
    public Optional<Task> getTask(long infoId) {
        return taskRepository.findById(infoId);
    }

    @Override
    public List<Task> findTasks(List<Filter> filters) {
        filters = new ArrayList<>(filters);
        return taskRepository.findByFilters(filters);
    }

    @Override
    @Transactional
    public void deleteTask(long taskId) {
        Task taskInfo = getTask(taskId).orElse(null);
        if (taskInfo != null) {
            for (TaskNode taskNode : getParentNodes(taskId)) {
                unlinkTaskNode(taskNode);
            }

            taskRepository.deleteById(taskId);
        }
    }

    @Override
    public List<TaskNode> getChildNodes(long taskId) {
        List<TaskNode> unorderedChildren = nodeRepository.findByFilters(List.of(Filter.builder()
                .field(TaskNode_.PARENT)
                .operator(QueryOperator.EQ_TASK)
                .value(taskId)
                .build()
        ));
        List<TaskNode> orderedChildren = new ArrayList<>();
        if (!unorderedChildren.isEmpty()) {
            TaskNode head = null;
            for (TaskNode child : unorderedChildren) {
                if (child.getPrev() == null) {
                    head = child;
                    break;
                }
            }
            if (head != null) {
                TaskNode current = head;
                while (current != null) {
                    orderedChildren.add(current);
                    current = current.getNext();
                }
            } else throw new RuntimeException("Fetched child nodes are malformed.");
        }
        return orderedChildren;
    }

    @Override
    public Set<TaskNode> getParentNodes(long taskId) {
        return new HashSet<>(nodeRepository.findByFilters(List.of(Filter.builder()
                        .field(TaskNode_.DATA)
                        .operator(QueryOperator.EQUALS)
                        .value(taskId)
                        .build()
        )));
    }

    @Override
    @Transactional
    public TaskNode appendNodeTo(long childTaskId, long parentTaskId) {
        TaskNode newNode = TaskNode.builder()
                .data(getTask(childTaskId).orElseThrow())
                .parent(getTask(parentTaskId).orElse(null))
                .build();
        List<TaskNode> children = new ArrayList<>();
        if (getTask(parentTaskId).orElse(null) != null) {
             children = getChildNodes(parentTaskId);
        }
        TaskNode prevNode = null;
        if (!children.isEmpty()) {
            prevNode = children.get(children.size() - 1);
            newNode.setPrev(prevNode);
            prevNode.setNext(newNode);
        }
        return nodeRepository.save(newNode);
    }

    @Override
    @Transactional
    public TaskNode insertNodeBefore(long taskId, long nextNodeId) {
        TaskNode newNode = TaskNode.builder()
                .data(getTask(taskId).orElseThrow())
                .next(getNode(nextNodeId).orElse(null))
                .build();
        TaskNode nextNode = newNode.getNext();
        if (nextNode != null) {
            if (nextNode.getParent() != null) {
                List<TaskNode> children = getChildNodes(nextNode.getParent().getId());
                newNode.setParent(nextNode.getParent());
                if (!children.isEmpty()) {
                    int position = children.indexOf(nextNode);
                    if (position > 0) {
                        TaskNode prevNode = children.get(position - 1);
                        newNode.setPrev(prevNode);
                        prevNode.setNext(newNode);
                    }
                }
            }
            nextNode.setPrev(newNode);
        }
        return nodeRepository.save(newNode);
   }

    @Override
    public Optional<TaskNode> getNode(long id) {
        return nodeRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteNode(long nodeId) {
        Optional<TaskNode> taskNodeOptional = nodeRepository.findById(nodeId);
        if (taskNodeOptional.isPresent()) {
            TaskNode taskNode = taskNodeOptional.get();
            unlinkTaskNode(taskNode);
            nodeRepository.deleteById(nodeId);
        }
    }

    private void linkTaskNodes(TaskNode first, TaskNode second) {
        first.setNext(second);
        second.setPrev(first);
    }

    private void unlinkTaskNode(TaskNode taskNode) {
        TaskNode prev = taskNode.getPrev();
        TaskNode next = taskNode.getNext();

        if (prev != null) {
            prev.setNext(next);
            nodeRepository.save(prev);
        }

        if (next != null) {
            next.setPrev(prev);
            nodeRepository.save(next);
        }
    }

}