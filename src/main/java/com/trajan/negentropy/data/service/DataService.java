package com.trajan.negentropy.data.service;

import com.trajan.negentropy.data.entity.Tag;
import com.trajan.negentropy.data.entity.Task;
import com.trajan.negentropy.data.repository.Filter;
import com.trajan.negentropy.data.repository.FilteredRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("service")
@RequiredArgsConstructor
public class DataService {
    private static final Logger logger = LoggerFactory.getLogger(DataService.class);
    @Autowired
    private final FilteredRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    // Tasks

    public List<Task> findTasks(List<Filter> filters) {
        List<Task> tasks = repository.getTaskQueryResult(filters);
        logger.debug("Fetched tasks with size: {}", tasks.size());
        return tasks;
    }

    public Task getTask(int id) {
        return repository.getTaskRepository().getReferenceById(id);
    }

    public Task getTask(Task task) {
        return getTask(task.getId());
    }

    public int countTasks() {
        return (int) repository.getTaskRepository().count();
    }

    @Transactional
    public void saveTask(Task task) {
        logger.debug("== SAVING TASK ==");
        task.log();
        if (!entityManager.contains(task)) {
            task = entityManager.merge(task);
        } else {
            entityManager.persist(task);
        }
        repository.getTaskRepository().save(task);
    }

    public void deleteTask(Task task) {
          repository.getTaskRepository().delete(task);
    }

    // Tags

    public List<Tag> findTags(List<Filter> filters) {
        return repository.getTagQueryResult(filters);
    }

    public Tag getTag(int id) {
        return repository.getTagRepository().getReferenceById(id);
    }

    public int countTags() {
        return (int) repository.getTagRepository().count();
    }

    public void saveTag(Tag tag) {
        if (tag.getId() != null && !entityManager.contains(tag)) {
            tag = entityManager.merge(tag);
        } else {
            entityManager.persist(tag);
        }
        repository.getTagRepository().save(tag);
    }

    public void deleteTag(Tag task) {
        repository.getTagRepository().delete(task);
    }
}