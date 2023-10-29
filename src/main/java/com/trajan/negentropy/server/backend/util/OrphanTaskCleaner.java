package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Slf4j
public class OrphanTaskCleaner {
    @Autowired private EntityQueryService entityQueryService;
    @Autowired private TaskRepository taskRepository;

    public void deleteAllOrphanedTasks() {
        entityQueryService.findOrphanedTasks().forEach(task -> {
            log.debug("Deleting orphaned task: " + task);
            try {
                taskRepository.delete(task);
            } catch (Throwable t) {
                log.error("Error deleting orphaned task: " + task, t);
            }
        });
    }
}
