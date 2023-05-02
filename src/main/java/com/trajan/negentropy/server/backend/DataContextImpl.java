package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.repository.LinkRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Transactional
public class DataContextImpl implements DataContext {
    private static final Logger logger = LoggerFactory.getLogger(DataContext.class);

    @Autowired private TaskRepository taskRepository;
    @Autowired private LinkRepository linkRepository;

    @Override
    public TaskEntity updateTask(TaskEntity fresh) {
        if (!fresh.exists()) {
            return taskRepository.save(fresh);
        } else throw new IllegalArgumentException(
                "updateTask cannot be called for a task that does not exist in the repository");
    }

    @Override
    public TaskEntity createTask(TaskEntity fresh) {
        if (!fresh.exists()) {
            return taskRepository.save(fresh);
        } else throw new IllegalArgumentException(
                "createTask cannot be called for a task that already exists in the repository");
    }

    @Override
    public TaskLink createLink(TaskLink fresh) {
        if (!fresh.exists()) {
            return linkRepository.save(fresh);
        } else throw new IllegalArgumentException(
                "createLink cannot be called for a task link that already exists in the repository");
    }

    @Override
    public TaskLink updateLink(TaskLink fresh) {
        if (!fresh.exists()) {
            return linkRepository.save(fresh);
        } else throw new IllegalArgumentException(
                "createLink cannot be called for a task that does not exist in the repository");
    }

    @Override
    public void deleteTask(TaskEntity task) {
        //timeEstimator.onTaskDeleted(task);
        //            Pair<Set<TaskLink>, Set<Task>> nodesWhereTaskIsAncestor = queryService.getDescendantNodes(task)
//                    .unordered()
//                    .collect(Collectors.teeing(
//                            Collectors.toSet(),
//                            Collectors.filtering(
//                                    // TODO: queryService.isAncestor?
//                                    link -> !queryService.hasParents(link.child()),
//                                    Collectors.mapping(TaskLink::child, Collectors.toSet())),
//                            Pair::of));
//
//            logger.debug("Deleting " + nodesWhereTaskIsAncestor.getFirst().size() +
//                    " nodes along with " + nodesWhereTaskIsAncestor.getSecond().size() + " tasks");
//            dataContext.deleteNodes(nodesWhereTaskIsAncestor.getFirst());
//            dataContext.deleteTasks(nodesWhereTaskIsAncestor.getSecond());
//
//            Set<TaskLink> nodesWhereTaskIsChild = queryService.getAllReferenceNodes(task)
//                    .collect(Collectors.toSet());
//
//
//
//            dataContext.deleteNodes(nodesWhereTaskIsChild);
//            dataContext.deleteTask(task);
        taskRepository.delete(task);
    }

    @Override
    public void deleteTasks(Set<TaskEntity> tasks) {
        taskRepository.deleteAll(tasks);
    }
}
