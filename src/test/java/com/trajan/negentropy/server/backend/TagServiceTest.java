package com.trajan.negentropy.server.backend;

import com.google.common.collect.ImmutableList;
import com.trajan.negentropy.model.TagGroup;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.TagGroupChange;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
class TagServiceTest extends TaskTestTemplate {
    @Autowired private TagService tagService;

    public static final String REPEATTIMES = "repeatTimes";
    private TagGroup tagGroup;

    @BeforeAll
    void setup() {
        init();

        Change persistTagGroup = new Change.PersistChange<>(new TagGroup(
                null,
                REPEATTIMES,
                ""));

        DataMapResponse response = changeService.execute(Request.of(persistTagGroup));
        assertTrue(response.success());
        tagGroup = (TagGroup) response.changeRelevantDataMap().get(persistTagGroup.id()).get(0);

        response = changeService.execute(Request.of(
                new TagGroupChange(
                        tags.get(REPEATONCE).id(),
                        tagGroup.id(),
                        TagGroupChange.TagGroupChangeType.ADD),
                new TagGroupChange(
                        tags.get(REPEATSEVERAL).id(),
                        tagGroup.id(),
                        TagGroupChange.TagGroupChangeType.ADD)));
        assertTrue(response.success());

        List<TagEntity> tags = ImmutableList.copyOf(tagService.getTagsByTagGroupId(tagGroup.id()));
        assertEquals(2, tags.size());
    }

    @Test
    @Transactional
    void testRemoveTagFromGroup() {
        changeService.execute(Request.of(
                new TagGroupChange(
                        tags.get(REPEATSEVERAL).id(),
                        tagGroup.id(),
                        TagGroupChange.TagGroupChangeType.REMOVE)));

        assertFalse(tagService.getTagGroupMap().get(tagGroup).contains(tags.get(REPEATSEVERAL)));
    }

    @Test
    @Disabled
    void testTagMap() {
        assertEquals(2, tagService.getTagGroupMap().get(tagGroup).size());
        assertTrue(tagService.getTagGroupMap().get(tagGroup).contains(tags.get(REPEATONCE)));
        assertTrue(tagService.getTagGroupMap().get(tagGroup).contains(tags.get(REPEATSEVERAL)));
    }
}