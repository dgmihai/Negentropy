package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskRelationship;
import com.trajan.negentropy.server.repository.Filter;
import com.trajan.negentropy.server.repository.FilteredTaskInfoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Transactional
@Service("taskInfoService")
public class TaskInfoService implements GenericDataService<TaskInfo> {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfoService.class);

    private final FilteredTaskInfoRepository filteredTaskInfoRepository;
    private final TaskRelationshipService taskRelationshipService;

    public TaskInfoService(FilteredTaskInfoRepository filteredTaskInfoRepository, TaskRelationshipService taskRelationshipService) {
        this.filteredTaskInfoRepository = filteredTaskInfoRepository;
        this.taskRelationshipService = taskRelationshipService;
    }

    @Override
    @Transactional
    public Response save(TaskInfo taskInfo) {
        try {
            if (taskInfo.getId() == null) {
                // Adding a brand new TaskInfo
                if (taskInfo.getRelationships().isEmpty()) {
                    // Add a new relationship with null parent and empty children
                    TaskRelationship newRelationship = new TaskRelationship();
                    newRelationship.setTaskInfo(taskInfo);
                    taskInfo.addRelationship(newRelationship);
                }
            } else {
                // Updating an existing TaskInfo
                for (TaskRelationship relationship : taskInfo.getRelationships()) {
                    relationship.setTaskInfo(taskInfo);
                }
            }

            // Save the TaskInfo instance and its associated TaskRelationship instances
            filteredTaskInfoRepository.getTaskInfoRepository().save(taskInfo);

            return new Response();
        } catch (Exception e) {
            return new Response(e);
        }
    }

    @Override
    public TaskInfo findById(Long id) {
        Optional<TaskInfo> taskInfo = filteredTaskInfoRepository.getTaskInfoRepository().findById(id);
        if (taskInfo.isEmpty()) {
            logger.info("Attempted to fetch null taskInfo with id: {}", id);
            return null;
        } else {
            return taskInfo.orElse(null);
        }
    }

    @Override
    public List<TaskInfo> find(List<Filter> filters) {
        return filteredTaskInfoRepository.findByFilters(filters);
    }

    @Override
    public Response delete(TaskInfo entity) {
        try {
            filteredTaskInfoRepository.getTaskInfoRepository().delete(entity);
            return new Response();
        } catch (Exception e) {
            return new Response(e);
        }
    }

    @Override
    public List<TaskInfo> getAll() {
        return filteredTaskInfoRepository.getTaskInfoRepository().findAll();
    }
}