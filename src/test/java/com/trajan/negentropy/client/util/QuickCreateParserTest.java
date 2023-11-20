package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.components.quickcreate.QuickCreateParser;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task.TaskDTO;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.HasTaskNodeData.TaskNodeInfoData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QuickCreateParserTest {

    @Test
    void testBasicInput() {
        try {
            String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur 2m #rec #top";
            Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
            TaskDTO task = result.getFirst();
            TaskNodeDTO nodeDTO = (TaskNodeDTO) result.getSecond();

            assertEquals("TaskName", task.name());
            assertEquals("TaskDescription", task.description());
            assertEquals(Duration.ofMinutes(2), task.duration());
            assertTrue(nodeDTO.recurring());
            assertTrue(task.tags().contains(new Tag(null, "tag1")));
            assertTrue(task.tags().contains(new Tag(null, "tag2")));
            assertEquals(0, nodeDTO.position());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testIncompleteInput() {
        try {
            String input = "TaskName #desc TaskDescription";
            Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
            TaskDTO task = result.getFirst();
            TaskNodeDTO nodeDTO = (TaskNodeDTO) result.getSecond();

            assertEquals("TaskName", task.name());
            assertEquals("TaskDescription", task.description());
            assertTrue(task.duration().isZero());
            assertFalse(nodeDTO.recurring());
            assertNull(nodeDTO.position());
            assertTrue(task.tags().isEmpty());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testOutOfOrderInput() {
        try {
            String input = "TaskName #dur 2m #rec #desc TaskDescription #top #tag tag1, tag2";
            Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
            TaskDTO task = result.getFirst();
            TaskNodeDTO nodeDTO = (TaskNodeDTO) result.getSecond();

            assertEquals("TaskName", task.name());
            assertEquals("TaskDescription", task.description());
            assertEquals(Duration.ofMinutes(2), task.duration());
            assertTrue(nodeDTO.recurring());
            assertTrue(task.tags().contains(new Tag(null, "tag1")));
            assertTrue(task.tags().contains(new Tag(null, "tag2")));
            assertEquals(0, nodeDTO.position());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testSpacesInNameAndDescription() {
        try {
            String input = "Task Name with Spaces #desc Task Description with Spaces #tag tag1, tag2 #dur 2m #rec";
            Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
            TaskDTO task = result.getFirst();
            TaskNodeDTO nodeDTO = (TaskNodeDTO) result.getSecond();

            assertEquals("Task Name with Spaces", task.name());
            assertEquals("Task Description with Spaces", task.description());
            assertEquals(Duration.ofMinutes(2), task.duration());
            assertTrue(nodeDTO.recurring());
            assertTrue(task.tags().contains(new Tag(null, "tag1")));
            assertTrue(task.tags().contains(new Tag(null, "tag2")));
            assertNull(nodeDTO.position());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testInvalidDuration() {
        String input = "TaskName #desc TaskDescription #tag tag1, tag2 #dur not_a_number #rec";
        assertThrows(QuickCreateParser.ParseException.class, () -> QuickCreateParser.parse(input));
    }

    @Test
    void testOnlyName() {
        try {
            String input = "TaskName";
            Pair<TaskDTO, TaskNodeInfoData<TaskNodeDTO>> result = QuickCreateParser.parse(input);
            TaskDTO task = result.getFirst();
            TaskNodeDTO nodeDTO = (TaskNodeDTO) result.getSecond();

            assertEquals("TaskName", task.name());
            assertTrue(task.description().isBlank());
            assertTrue(task.duration().isZero());
            assertFalse(nodeDTO.recurring());
            assertTrue(task.tags().isEmpty());
            assertNull(nodeDTO.position());
        } catch (Exception e) {
            fail(e);
        }
    }
}
