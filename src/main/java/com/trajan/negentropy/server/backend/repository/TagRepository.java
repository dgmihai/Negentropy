package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository("tagRepository")
public interface TagRepository extends BaseRepository<TagEntity, Long> {
    Optional<TagEntity> findFirstByName(String name);

    List<TagEntity> findByIdIn(List<Long> tagIds);

    Stream<TagEntity> findByTasksId(Long taskId);

    Stream<TagEntity> findByTasks(TaskEntity task);
}

