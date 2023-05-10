package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import org.springframework.stereotype.Repository;

@Repository("tagRepository")
public interface TagRepository extends BaseRepository<TagEntity, Long> { }

