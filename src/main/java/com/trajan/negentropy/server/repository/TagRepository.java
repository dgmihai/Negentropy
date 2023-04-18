package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.Tag;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.trajan.negentropy.server.repository.filter.GenericSpecificationProvider;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("tagRepository")
@Transactional
public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag>, GenericSpecificationProvider<Tag> {
    default List<Tag> findAllFiltered(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<Tag> spec = getSpecificationFromFilters(filters, Tag.class);
            return findAll(spec);
        } else {
            return findAll();
        }
    }
}
