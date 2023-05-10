package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TaskEntity;
import org.springframework.stereotype.Repository;

/**
 * A repository interface for managing task entities.
 * @see TaskEntity
 */
@Repository("taskRepository")
public interface TaskRepository extends BaseRepository<TaskEntity, Long> { }
