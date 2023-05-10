package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TaskLink;
import org.springframework.stereotype.Repository;

/**
 * A repository interface for managing links between tasks.
 * </p>
 * A task link represents the relationship a task has to other tasks.
 * @see TaskLink
 */
@Repository("linkRepository")
public interface LinkRepository extends BaseRepository<TaskLink, Long> { }
