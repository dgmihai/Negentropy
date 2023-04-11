package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.Tag;
import com.trajan.negentropy.server.repository.Filter;
import com.trajan.negentropy.server.repository.FilteredTagRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Transactional
@Service("tagService")
public class TagService implements GenericDataService<Tag> {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final FilteredTagRepository filteredTagRepository;

    public TagService(FilteredTagRepository filteredTagRepository) {
        this.filteredTagRepository = filteredTagRepository;
    }

    @Override
    public Response save(Tag entity) {
        try {
            if (entity.getId() == null) {
                filteredTagRepository.getTagRepository().save(entity);
            } else {
                filteredTagRepository.getTagRepository().saveAndFlush(entity);
            }
            return new Response();
        } catch (Exception e) {
            return new Response(e.getMessage());
        }
    }

    @Override
    public Tag findById(Long id) {
        return filteredTagRepository.getTagRepository().findById(id).orElse(null);
    }

    @Override
    public List<Tag> find(List<Filter> filters) {
        return filteredTagRepository.findByFilters(filters);
    }

    @Override
    public Response delete(Tag entity) {
        try {
            filteredTagRepository.getTagRepository().delete(entity);
            return new Response();
        } catch (Exception e) {
            return new Response(e.getMessage());
        }
    }

    @Override
    public List<Tag> getAll() {
        return filteredTagRepository.getTagRepository().findAll();
    }
}