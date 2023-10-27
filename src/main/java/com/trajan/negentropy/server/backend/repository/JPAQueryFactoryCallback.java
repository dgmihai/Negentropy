package com.trajan.negentropy.server.backend.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

@FunctionalInterface
public interface JPAQueryFactoryCallback<T> {
    JPAQuery<T> doWithJPAQueryFactory(JPAQueryFactory queryFactory);
}
