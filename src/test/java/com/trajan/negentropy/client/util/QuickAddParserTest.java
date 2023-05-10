package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.components.quickadd.QuickAddParser;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuickAddParserTest {

    @Test
    void testBasicInput() {
        String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur 120 #rep";
        Task task = QuickAddParser.parse(input);

        assertEquals("TaskName", task.name());
        assertEquals("TaskDescription", task.description());
        assertEquals(Duration.ofSeconds(120), task.duration());
        assertFalse(task.oneTime());
        assertTrue(task.tags().contains(new Tag(null, "tag1")));
        assertTrue(task.tags().contains(new Tag(null, "tag2")));
    }

    @Test
    void testIncompleteInput() {
        String input = "TaskName #desc TaskDescription";
        Task task = QuickAddParser.parse(input);

        assertEquals("TaskName", task.name());
        assertEquals("TaskDescription", task.description());
        assertTrue(task.duration().isZero());
        assertTrue(task.oneTime());
        assertTrue(task.tags().isEmpty());
    }

    @Test
    void testOutOfOrderInput() {
        String input = "TaskName #dur 120 #rep #desc TaskDescription #tag tag1, tag2";
        Task task = QuickAddParser.parse(input);

        assertEquals("TaskName", task.name());
        assertEquals("TaskDescription", task.description());
        assertEquals(Duration.ofSeconds(120), task.duration());
        assertFalse(task.oneTime());
        assertTrue(task.tags().contains(new Tag(null, "tag1")));
        assertTrue(task.tags().contains(new Tag(null, "tag2")));
    }

    @Test
    void testSpacesInNameAndDescription() {
        String input = "Task Name with Spaces #desc Task Description with Spaces #tag tag1, tag2 #dur 120 #rep";
        Task task = QuickAddParser.parse(input);

        assertEquals("Task Name with Spaces", task.name());
        assertEquals("Task Description with Spaces", task.description());
        assertEquals(Duration.ofSeconds(120), task.duration());
        assertFalse(task.oneTime());
        assertTrue(task.tags().contains(new Tag(null, "tag1")));
        assertTrue(task.tags().contains(new Tag(null, "tag2")));
    }

    @Test
    void testInvalidDuration() {
        String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur not_a_number #rep";
        assertThrows(IllegalStateException.class, () -> QuickAddParser.parse(input));
    }

    @Test
    void testOnlyName() {
        String input = "TaskName";
        Task task = QuickAddParser.parse(input);

        assertEquals("TaskName", task.name());
        assertTrue(task.description().isBlank());
        assertTrue(task.duration().isZero());
        assertTrue(task.oneTime());
        assertTrue(task.tags().isEmpty());
    }
}
