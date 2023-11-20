package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TaskEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.stream.Stream;

/**
 * A repository interface for managing task entities.
 * @see TaskEntity
 */
@Repository("taskRepository")
public interface TaskRepository extends BaseRepository<TaskEntity, Long> {

    @Query("SELECT t.id FROM TaskEntity t JOIN t.tags tag WHERE tag.id = :tagId")
    Stream<Long> findTaskIdsByTagId(Long tagId);
}
