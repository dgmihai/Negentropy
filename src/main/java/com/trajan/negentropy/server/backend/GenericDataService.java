package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.AbstractEntity;
import com.trajan.negentropy.server.backend.repository.filter.Filter;
import jakarta.transaction.Transactional;

import java.util.List;

@Transactional
public interface GenericDataService<T extends AbstractEntity> {
    T create(T entity);

    T update(T entity);

    T findById(long id);

    List<T> find(List<Filter> filters);

    void delete(T entity);
}