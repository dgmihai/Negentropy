package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.AbstractEntity;
import com.trajan.negentropy.server.repository.Filter;

import java.util.List;

public interface GenericDataService<T extends AbstractEntity> {
    Response save(T entity);

    T findById(Long id);

    List<T> find(List<Filter> filters);

    Response delete(T entity);

    List<T> getAll();
}