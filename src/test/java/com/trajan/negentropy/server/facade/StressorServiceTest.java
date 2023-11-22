package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Stressor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class StressorServiceTest {
    @Autowired private StressorService stressorService;

    private Stressor createStressor() {
        return stressorService.persist(new Stressor("Stressor"));
    }

    @Test
    void save() {
        Stressor stressor = createStressor();
        assertNotNull(stressor.id());
        assertEquals("Stressor", stressor.name());
    }

    @Test
    @Disabled
    void saveDuplicate() {
        Stressor stressor = createStressor();
        Long id1 = stressor.id();
        stressor.id(null);
        stressor = stressorService.persist(stressor);
        Long id2 = stressor.id();
        assertEquals(id1, id2);
    }

    @Test
    void record() {
        Stressor stressor = createStressor();
        LocalDateTime time = LocalDateTime.now();
        stressorService.record(stressor.id(), time);
        // TODO: Way to use/retrieve stressor timestamps
    }

    @Test
    void delete() {
        Stressor stressor = createStressor();
        stressorService.delete(stressor.id());
        assertThrows(Exception.class, () -> stressorService.get(stressor.id()));
    }

    @Test
    void get() {
        Stressor stressor = createStressor();
        Stressor retrieved = stressorService.get(stressor.id());
        assertEquals(stressor.id(), retrieved.id());
        assertEquals(stressor.name(), retrieved.name());
    }

    @Test
    void getNonexistant() {
        assertThrows(Exception.class, () -> stressorService.get(0L));
    }
}