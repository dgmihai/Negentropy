package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.Tag;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Repository
@Transactional
public class FilteredTagRepository implements GenericSpecificationProvider<Tag> {
    @Autowired
    private TagRepository tagRepository;

    public List<Tag> findByFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<Tag> spec = getSpecificationFromFilters(filters, Tag.class);
            return tagRepository.findAll(spec);
        } else {
            return tagRepository.findAll();
        }
    }
}