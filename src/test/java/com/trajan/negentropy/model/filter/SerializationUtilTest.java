package com.trajan.negentropy.model.filter;

import com.trajan.negentropy.model.id.TagID;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SerializationUtilTest {

    private void assertSerialization(TaskNodeTreeFilter filter) throws Exception {
        String serializedFilter = SerializationUtil.serialize(filter);
        TaskNodeTreeFilter deserializedFilter = (TaskNodeTreeFilter) SerializationUtil.deserialize(serializedFilter);

        assertEquals(filter, deserializedFilter);

        TaskNodeTreeFilter falseFilter = new TaskNodeTreeFilter()
                .name("false");

        assertNotEquals(filter, falseFilter);
    }

    @Test
    void testSerialization() throws Exception {
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter()
//                .durationLimit(Duration.ofHours(3))
                .name("test")
                .completed(false)
                .availableAtTime(LocalDateTime.now());

        filter.options.add(TaskNodeTreeFilter.INNER_JOIN_INCLUDED_TAGS);

        assertSerialization(filter);
    }

    @Test
    void testSerializationEmpty() throws Exception {
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter();

        assertSerialization(filter);
    }

    @Test
    void testSerialization2() throws Exception {
        TaskNodeTreeFilter filter = new TaskNodeTreeFilter()
                .completed(false)
                .includedTagIds(Set.of(new TagID(1L)))
                .excludedTagIds(Set.of(new TagID(2L)));

        assertSerialization(filter);
    }

}