package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.Tag;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import com.trajan.negentropy.server.facade.model.TaskNodeDTO;
import com.trajan.negentropy.server.facade.model.id.ID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.helger.commons.mock.CommonsAssert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class DataContextTest extends TaskTestTemplate {
    @Autowired private DataContext dataContext;

    @Test
    void testTaskEntityToDTO() {
        TaskEntity taskEntity = dataContext.mergeTask(new TaskEntity()
                .name("Task Name")
                .description("Task Description")
                .duration(Duration.ofMinutes(120))
                .oneTime(true));

        Set<TagEntity> tagEntities = Set.of(
                dataContext.mergeTag(new TagEntity().name("Tag1")),
                dataContext.mergeTag(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(tagEntities);

        Task task = DataContext.toDTO(taskEntity);

        assertNotNull(task);
        assertEquals(ID.of(taskEntity), task.id());
        assertEquals(taskEntity.name(), task.name());
        assertEquals(taskEntity.description(), task.description());
        assertEquals(taskEntity.duration(), task.duration());
        assertEquals(taskEntity.oneTime().booleanValue(), task.oneTime());
        assertEquals(tagEntities.size(), task.tags().size());
        task.tags().forEach(tag -> {
            TagEntity originalTag = tagEntities.stream()
                    .filter(tagEntity ->ID.of(tagEntity).equals(tag.id()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(originalTag);
            assertEquals(originalTag.name(), tag.name());
        });
    }

    @Test
    void testTagEntityToDTO() {
        TagEntity tagEntity = dataContext.mergeTag(new TagEntity().name("Tag Name"));
        Tag tag = DataContext.toDTO(tagEntity);

        assertNotNull(tag);
        assertEquals(ID.of(tagEntity), tag.id());
        assertEquals(tagEntity.name(), tag.name());
    }

    @Test
    void testTaskLinkToDTO() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1));

        TaskLink nextLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(2));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNotNull(taskNode);
        assertEquals(ID.of(taskLink), taskNode.linkId());
        assertEquals(taskLink.importance(), taskNode.importance());
        assertEquals(taskLink.position(), taskNode.position());
        assertEquals(ID.of(parentTaskEntity), taskNode.parentId());
        assertEquals(ID.of(childTaskEntity), taskNode.childId());
    }

    @Test
    void testTaskLinkToDTONoPrev() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(0));

        TaskLink nextLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNotNull(taskNode);
        assertEquals(ID.of(taskLink), taskNode.linkId());
        assertEquals(taskLink.importance(), (Integer) taskNode.importance());
        assertEquals(taskLink.position(), taskNode.position());
        assertEquals(ID.of(parentTaskEntity), taskNode.parentId());
        assertEquals(ID.of(childTaskEntity), taskNode.childId());
    }

    @Test
    void testTaskLinkToDTONoNext() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNotNull(taskNode);
        assertEquals(ID.of(taskLink), taskNode.linkId());
        assertEquals(taskLink.importance(), (Integer) taskNode.importance());
        assertEquals(taskLink.position(), taskNode.position());
        assertEquals(ID.of(parentTaskEntity), taskNode.parentId());
        assertEquals(ID.of(childTaskEntity), taskNode.childId());
    }

    @Test
    void testTaskMerge() {
        TaskEntity taskEntity = dataContext.mergeTask(new TaskEntity()
                .name("Original Task Name")
                .description("Original Task Description")
                .duration(Duration.ofMinutes(120))
                .oneTime(true));

        Set<TagEntity> originalTagEntities = Set.of(
                dataContext.mergeTag(new TagEntity().name("Tag1")),
                dataContext.mergeTag(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(originalTagEntities);

        Task task = new Task(ID.of(taskEntity),
                "Updated Task Name",
                "Updated Task Description",
                Duration.ofMinutes(180),
                originalTagEntities.stream().map(DataContext::toDTO).collect(Collectors.toSet()),
                false);

        TaskEntity mergedTaskEntity = dataContext.mergeTask(task);

        assertEquals(task.id(), ID.of(mergedTaskEntity));
        assertEquals(task.name(), mergedTaskEntity.name());
        assertEquals(task.description(), mergedTaskEntity.description());
        assertEquals(task.duration(), mergedTaskEntity.duration());
        assertEquals(task.oneTime(), mergedTaskEntity.oneTime());
        assertEquals(task.tags().size(), mergedTaskEntity.tags().size());
    }

    @Test
    void testTagMerge() {
        TagEntity tagEntity = dataContext.mergeTag(new TagEntity().name("Original Tag Name"));
        Tag tag = new Tag(ID.of(tagEntity), "Updated Tag Name");

        TagEntity mergedTagEntity = dataContext.mergeTag(tag);

        assertEquals(tag.id(), ID.of(mergedTagEntity));
        assertEquals(tag.name(), mergedTagEntity.name());
    }

    @Test
    void testTaskLinkMerge() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1));

        TaskLink nextLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(2));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                ID.of(childTaskEntity),
                1,
                2);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertEquals(taskNode.linkId(), ID.of(mergedTaskLink));
        assertEquals(taskNode.importance(), mergedTaskLink.importance());
        assertEquals(taskNode.position(), mergedTaskLink.position());
        assertEquals(ID.of(parentTaskEntity), ID.of(mergedTaskLink.parent()));
        assertEquals(ID.of(childTaskEntity), ID.of(mergedTaskLink.child()));
    }

    @Test
    void testTaskLinkMergeInvalid() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink nextLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        assertThrows(IllegalArgumentException.class, () -> dataContext.mergeNode(
                new TaskNodeDTO()
                        .parentId(ID.of(parentTaskEntity)).childId(ID.of(childTaskEntity)).position(4).importance(2)));

        parentTaskEntity.childLinks(List.of(prevLink, nextLink));
    }

    @Test
    void testTaskLinkMergeNoNext() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                ID.of(childTaskEntity),
                1,
                2);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertEquals(taskNode.linkId(), ID.of(mergedTaskLink));
        assertEquals(taskNode.importance(), mergedTaskLink.importance());
        assertEquals(taskNode.position(), mergedTaskLink.position());
        assertEquals(ID.of(parentTaskEntity), ID.of(mergedTaskLink.parent()));
        assertEquals(ID.of(childTaskEntity), ID.of(mergedTaskLink.child()));
    }

    @Test
    void testTaskLinkMergeNoPrev() {
        TaskEntity parentTaskEntity = dataContext.mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.mergeTask(new TaskEntity().name("Child Task"));

        TaskLink taskLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(0));

        TaskLink nextLink = dataContext.mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                ID.of(childTaskEntity),
                0,
                2);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertEquals(taskNode.linkId(), ID.of(mergedTaskLink));
        assertEquals(taskNode.importance(), mergedTaskLink.importance());
        assertEquals(taskNode.position(), mergedTaskLink.position());
        assertEquals(ID.of(parentTaskEntity), ID.of(mergedTaskLink.parent()));
        assertEquals(ID.of(childTaskEntity), ID.of(mergedTaskLink.child()));
    }
}
