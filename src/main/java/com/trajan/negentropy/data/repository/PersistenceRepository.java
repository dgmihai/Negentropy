package com.trajan.negentropy.data.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;

@Getter
public class PersistenceRepository {
    @PersistenceContext
    EntityManager entityManager;
}
