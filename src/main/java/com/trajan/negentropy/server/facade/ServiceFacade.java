package com.trajan.negentropy.server.facade;

import java.util.stream.Stream;

public interface ServiceFacade<T> {
    T persist(T data);
    void delete(Long id);
    T get(Long id);
    Stream<T> getAll();
}