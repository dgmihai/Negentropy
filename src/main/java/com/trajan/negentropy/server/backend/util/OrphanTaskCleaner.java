package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.model.entity.routine.QRoutineStepEntity;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
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
    @Autowired private RoutineStepRepository stepRepository;

    public void deleteAllOrphanedTasks() {
        entityQueryService.findOrphanedTasks().forEach(task -> {
            if (!stepRepository.exists(QRoutineStepEntity.routineStepEntity.task.eq(task))) {
                log.debug("Deleting orphaned task <" + task + ">");
                try {
                    taskRepository.delete(task);
                } catch (Throwable t) {
                    log.error("Error deleting orphaned task: " + task, t);
                }
            } else {
                log.debug("Skipping orphaned task <" + task.name() + "> because it is referred to by routine steps");
            }
        });
    }
}
