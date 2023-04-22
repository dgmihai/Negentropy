package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.TaskNodeRepository;
import com.trajan.negentropy.server.repository.TaskRepository;
import com.trajan.negentropy.server.repository.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service("taskService")
public class TaskServiceImpl implements TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final TaskRepository taskRepository;
    private final TaskNodeRepository nodeRepository;

    public TaskServiceImpl(TaskRepository taskRepository, TaskNodeRepository nodeRepository) {
        this.taskRepository = taskRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    @Transactional
    public Pair<Task, TaskNode> createTaskWithNode(Task task) {
        task = createTask(task);
        TaskNode node = createOrphanNode(task.getId());
        return Pair.of(task, node);
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
        return taskRepository.findAllFiltered(new ArrayList<>(filters));
    }

    @Override
    @Transactional
    public void deleteTask(long taskId) {
        Task taskInfo = getTask(taskId).orElse(null);
        if (taskInfo != null) {
            List<Task> checkForPurge = new ArrayList<>();
            for (TaskNode taskNode : getReferenceNodes(taskId)) {
                unlinkTaskNode(taskNode);
                nodeRepository.delete(taskNode);
            }
            for (TaskNode taskNode : getChildNodes(taskId)) {
                checkForPurge.add(taskNode.getReferenceTask());
                nodeRepository.delete(taskNode);
            }
            taskRepository.deleteById(taskId);
            purgeTasksIfOrphaned(checkForPurge);
        }
    }

    @Transactional
    private void purgeTasksIfOrphaned(List<Task> tasks) {
        for (Task task : tasks) {
            if(getReferenceNodes(task.getId()).isEmpty()) {
                deleteTask(task.getId());
            }
        }
    }

    @Override
    public List<TaskNode> getOrphanNodes() {
        return nodeRepository.findByParentTask(null);
    }

    @Override
    public int countOrphanNodes() {
        return nodeRepository.countByParentTask(null);
    }

    @Override
    public List<TaskNode> getChildNodes(long taskId) {
          return nodeRepository.orderNodes(nodeRepository.findByParentTask(getTask(taskId).orElseThrow()));
    }

    @Override
    public int countChildNodes(long taskId) {
        return nodeRepository.countByParentTask(getTask(taskId).orElseThrow());
    }

    @Override
    public List<TaskNode> findChildNodes(long taskId, List<Filter> filters) {
        return nodeRepository.findByParentFiltered(getTask(taskId).orElseThrow(), filters);
    }

    @Override
    public Set<TaskNode> getReferenceNodes(long taskId) {
        return new HashSet<>(nodeRepository.findByReferenceTask(getTask(taskId).orElseThrow()));
    }

    @Override
    @Transactional
    public TaskNode createChildNode(long parentTaskId, long childTaskId, int priority) {
        TaskNode newNode = TaskNode.builder()
                .referenceTask(getTask(childTaskId).orElseThrow())
                .parentTask(getTask(parentTaskId).orElseThrow())
                .priority(priority)
                .build();
        List<TaskNode> children = getChildNodes(parentTaskId);
        if (!children.isEmpty()) {
            TaskNode prevNode = children.get(children.size() - 1);
            newNode.setPrev(prevNode);
            prevNode.setNext(newNode);
        }
        return nodeRepository.save(newNode);
    }

    @Override
    @Transactional
    public TaskNode createNodeBefore(long taskId, long nextNodeId, int priority) {
        TaskNode newNode = TaskNode.builder()
                .referenceTask(getTask(taskId).orElseThrow())
                .next(getNode(nextNodeId).orElseThrow())
                .priority(priority)
                .build();
        TaskNode nextNode = newNode.getNext();
        if (nextNode != null) {
            if (nextNode.getParentTask() != null) {
                newNode.setParentTask(nextNode.getParentTask());
                linkNodes(nextNode.getPrev(), newNode, nextNode);
            } else {
                //TODO: Behavior when trying to link to a node with no parent?
            }
        } else {
            throw new IllegalArgumentException("Unable to create new node before id " + nextNodeId +
                    ": id refers to no existing node.");
        }
        return nodeRepository.save(newNode);
    }

    @Override
    @Transactional
    public TaskNode createNodeAfter(long taskId, long prevNodeId, int priority) {
        TaskNode newNode = TaskNode.builder()
                .referenceTask(getTask(taskId).orElseThrow())
                .prev(getNode(prevNodeId).orElseThrow())
                .priority(priority)
                .build();
        TaskNode prevNode = newNode.getPrev();
        if (prevNode != null) {
            if (prevNode.getParentTask() != null) {
                newNode.setParentTask(prevNode.getParentTask());
                linkNodes(prevNode, newNode, prevNode.getNext());
            } else {
                //TODO: Behavior when trying to link to a node with no parent?
            }
        } else {
            throw new IllegalArgumentException("Unable to create new node after id " + prevNodeId +
                    ": id refers to no existing node.");
        }
        return nodeRepository.save(newNode);
    }

    private void linkNodes(TaskNode prev, TaskNode newNode, TaskNode next) {
        newNode.setPrev(prev);
        newNode.setNext(next);
        if (next != null) {
            next.setPrev(newNode);
        }
        if (prev != null) {
            prev.setNext(newNode);
        }
    }

    @Override
    public TaskNode createOrphanNode(long taskId) {
        return nodeRepository.save(TaskNode.builder()
                .referenceTask(getTask(taskId).orElseThrow())
                .build());
    }

    @Override
    public Optional<TaskNode> getNode(long id) {
        return nodeRepository.findById(id);
    }

    @Override
    public List<TaskNode> findAllNodes(List<Filter> filters) {
        return nodeRepository.findAllFiltered(new ArrayList<>(filters));
    }

    @Override
    @Transactional
    public void deleteNode(long nodeId) {
        Optional<TaskNode> taskNodeOptional = nodeRepository.findById(nodeId);
        if (taskNodeOptional.isPresent()) {
            TaskNode taskNode = taskNodeOptional.get();
            unlinkTaskNode(taskNode);
            nodeRepository.deleteById(nodeId);
            purgeTasksIfOrphaned(List.of(taskNode.getReferenceTask()));
        }
    }

    private void unlinkTaskNode(TaskNode taskNode) {
        TaskNode prev = taskNode.getPrev();
        TaskNode next = taskNode.getNext();

        if (prev != null) {
            prev.setNext(next);
        }

        if (next != null) {
            next.setPrev(prev);
        }
    }
}