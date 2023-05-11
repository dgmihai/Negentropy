package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.components.quickadd.QuickCreateParser;
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
        try {
             String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur 2m #rep";
             Task task = QuickCreateParser.parse(input);

             assertEquals("TaskName", task.name());
             assertEquals("TaskDescription", task.description());
             assertEquals(Duration.ofMinutes(2), task.duration());
             assertFalse(task.oneTime());
             assertTrue(task.tags().contains(new Tag(null, "tag1")));
             assertTrue(task.tags().contains(new Tag(null, "tag2")));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testIncompleteInput() {
        try {
            String input = "TaskName #desc TaskDescription";
            Task task = QuickCreateParser.parse(input);

            assertEquals("TaskName", task.name());
            assertEquals("TaskDescription", task.description());
            assertTrue(task.duration().isZero());
            assertTrue(task.oneTime());
            assertTrue(task.tags().isEmpty());
            } catch (Exception e) {
                fail(e);
            }
    }

    @Test
    void testOutOfOrderInput() {
        try {
            String input = "TaskName #dur 2m #rep #desc TaskDescription #tag tag1, tag2";
            Task task = QuickCreateParser.parse(input);

            assertEquals("TaskName", task.name());
            assertEquals("TaskDescription", task.description());
            assertEquals(Duration.ofMinutes(2), task.duration());
            assertFalse(task.oneTime());
            assertTrue(task.tags().contains(new Tag(null, "tag1")));
            assertTrue(task.tags().contains(new Tag(null, "tag2")));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testSpacesInNameAndDescription() {
        try {
            String input = "Task Name with Spaces #desc Task Description with Spaces #tag tag1, tag2 #dur 2m #rep";
            Task task = QuickCreateParser.parse(input);

            assertEquals("Task Name with Spaces", task.name());
            assertEquals("Task Description with Spaces", task.description());
            assertEquals(Duration.ofMinutes(2), task.duration());
            assertFalse(task.oneTime());
            assertTrue(task.tags().contains(new Tag(null, "tag1")));
            assertTrue(task.tags().contains(new Tag(null, "tag2")));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testInvalidDuration() {
        String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur not_a_number #rep";
        assertThrows(QuickCreateParser.ParseException.class, () -> QuickCreateParser.parse(input));
    }

    @Test
    void testOnlyName() {
        try {
            String input = "TaskName";
            Task task = QuickCreateParser.parse(input);

            assertEquals("TaskName", task.name());
            assertTrue(task.description().isBlank());
            assertTrue(task.duration().isZero());
            assertTrue(task.oneTime());
            assertTrue(task.tags().isEmpty());
        } catch (Exception e) {
            fail(e);
        }
    }
}
