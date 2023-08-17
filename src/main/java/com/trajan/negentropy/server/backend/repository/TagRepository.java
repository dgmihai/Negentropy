package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TagEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("tagRepository")
public interface TagRepository extends BaseRepository<TagEntity, Long> {
    Optional<TagEntity> findFirstByName(String name);
}

