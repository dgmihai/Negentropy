package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.AbstractEntity;
import com.trajan.negentropy.server.repository.filter.Filter;
import jakarta.transaction.Transactional;

import java.util.List;

@Transactional
public interface GenericDataService<T extends AbstractEntity> {
    T save(T entity);

    T findById(Long id);

    List<T> find(List<Filter> filters);

    T delete(T entity);
}