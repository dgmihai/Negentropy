package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Transactional
@Service("tagService")
public class TagService implements GenericDataService<TagEntity> {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public TagEntity create(TagEntity tag) {
        return tagRepository.save(tag);
    }

    @Override
    public TagEntity update(TagEntity tag) {
        return tagRepository.save(tag);
    }

    @Override
    public TagEntity findById(long tagId) {
        return tagRepository.findById(tagId).orElse(null);
    }

    @Override
    public List<TagEntity> find(List<Filter> filters) {
        return tagRepository.findAllWithFilters(filters);
    }

    public List<TagEntity> findAll() {
        return tagRepository.findAll();
    }

    @Override
    public void delete(TagEntity entity) {
        tagRepository.delete(entity);
    }
}