package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import com.trajan.negentropy.server.backend.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("tagRepository")
@Transactional
public interface TagRepository extends JpaRepository<TagEntity, Long>, JpaSpecificationExecutor<TagEntity>, GenericSpecificationProvider<TagEntity> {
    default List<TagEntity> findAllWithFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TagEntity> spec = getSpecificationFromFilters(filters, TagEntity.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }
}
