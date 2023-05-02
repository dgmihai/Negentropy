package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TagService implements GenericDataService<TagEntity> {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    @Autowired private TagRepository tagRepository;

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
        return tagRepository.findById(tagId).orElseThrow();
    }

    @Override
    public List<TagEntity> find(List<Filter> filters) {
        return tagRepository.findAllFiltered(filters);
    }

    @Override
    public List<TagEntity> findAllById(Iterable<Long> ids) {
        return tagRepository.findAllById(ids);
    }

    @Override
    public List<TagEntity> findAll() {
        return tagRepository.findAll();
    }

    @Override
    public void delete(TagEntity entity) {
        tagRepository.delete(entity);
    }
}