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
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static com.helger.commons.mock.CommonsAssert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class DataContextTest extends TaskTestTemplate {
    @Autowired private DataContext dataContext;

    private static final String DAILY_STRING = "@daily";
    private static final CronExpression DAILY_CRON = CronExpression.parse(DAILY_STRING);

    private static final LocalDateTime TIME = LocalDateTime.now();

    private void assertTask(TaskEntity taskEntity, Task task) {
        assertNotNull(task);
        assertNotNull(task.id());
        assertEquals(ID.of(taskEntity), task.id());
        assertEquals(taskEntity.name(), task.name());
        assertEquals(taskEntity.description(), task.description());
        assertEquals(taskEntity.duration(), task.duration());
        assertEquals(taskEntity.block(), task.block());
    }

    @Test
    void testTaskEntityToDTO() {
        TaskEntity taskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity()
                .name("Task Name")
                .description("Task Description")
                .duration(Duration.ofMinutes(120)))
                .block(true);

        Set<TagEntity> tagEntities = Set.of(
                dataContext.mergeTag(new TagEntity().name("Tag1")),
                dataContext.mergeTag(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(tagEntities);

        Task task = DataContext.toDTO(taskEntity);

        assertTask(taskEntity, task);
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

    private void assertNode(TaskLink taskLink, TaskNode taskNode) {
        assertNotNull(taskNode);
        assertEquals(ID.of(taskLink), taskNode.linkId());
        assertEquals(taskLink.importance(), taskNode.importance());
        assertEquals(taskLink.position(), taskNode.position());
        assertEquals(taskLink.completed(), taskNode.completed());
        assertEquals(taskLink.recurring(), taskNode.recurring());
        assertEquals(taskLink.cron(), taskNode.cron());
        assertEquals(taskLink.createdAt(), taskNode.createdAt());
        assertEquals(taskLink.scheduledFor(), taskNode.scheduledFor());
        assertEquals(ID.of(taskLink.parent()), taskNode.parentId());
        assertEquals(ID.of(taskLink.child()), taskNode.child().id());
    }

    @Test
    void testTaskLinkToDTO() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                parentTaskEntity,
                childTaskEntity,
                0,
                0,
                MARK,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                parentTaskEntity,
                childTaskEntity,
                2,
                1,
                MARK,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                parentTaskEntity,
                childTaskEntity,
                2,
                0,
                MARK,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskLinkToDTONoPrev() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                parentTaskEntity,
                childTaskEntity,
                2,
                0,
                MARK,
                true,
                false,
                DAILY_STRING,
                LocalDateTime.MIN));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                parentTaskEntity,
                childTaskEntity,
                1,
                0,
                MARK,
                true,
                false,
                DAILY_STRING,
                LocalDateTime.MIN));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskLinkToDTONoNext() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1)
                .cron(DAILY_STRING));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink));

        TaskNode taskNode = DataContext.toDTO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskMerge() {
        TaskEntity taskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity()
                .name("Original Task Name")
                .description("Original Task Description")
                .duration(Duration.ofMinutes(120))
                .block(true));

        Set<TagEntity> originalTagEntities = Set.of(
                dataContext.mergeTag(new TagEntity().name("Tag1")),
                dataContext.mergeTag(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(originalTagEntities);

        Task task = new Task(ID.of(taskEntity),
                "Updated Task Name",
                "Updated Task Description",
                Duration.ofMinutes(180),
                true,
                originalTagEntities.stream().map(DataContext::toDTO).collect(Collectors.toSet()),
                false);

        TaskEntity mergedTaskEntity = dataContext.mergeTask(task);

        assertTask(taskEntity, task);
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
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .createdAt(MARK)
                .position(1)
                .cron(DAILY_STRING)
                .scheduledFor(LocalDateTime.MIN));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(2));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                DataContext.toDTO(childTaskEntity),
                1,
                2,
                MARK,
                false,
                false,
                DAILY_CRON,
                LocalDateTime.MIN);

        assertNode(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertNode(mergedTaskLink, taskNode);
    }

    @Test
    void testTaskLinkMergeInvalid() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        assertNotNull(ID.of(childTaskEntity));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        assertThrows(NoSuchElementException.class, () -> dataContext.mergeNode(
                new TaskNodeDTO(
                        ID.of(parentTaskEntity),
                        ID.of(childTaskEntity),
                        4,
                        2,
                        false,
                        false,
                        DAILY_CRON)));
    }

    @Test
    void testTaskLinkMergeNoNext() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(0));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(1)
                .createdAt(MARK)
                .completed(true)
                .recurring(false)
                .cron(DAILY_CRON)
                .scheduledFor(LocalDateTime.MIN));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                DataContext.toDTO(childTaskEntity),
                1,
                2,
                MARK,
                true,
                false,
                DAILY_CRON,
                LocalDateTime.MIN);

        assertNode(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertNode(mergedTaskLink, taskNode);
    }

    @Test
    void testTaskLinkMergeNoPrev() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .importance(2)
                .position(0)
                .createdAt(MARK)
                .completed(true)
                .recurring(false)
                .cron(DAILY_CRON)
                .scheduledFor(LocalDateTime.MIN));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                DataContext.toDTO(childTaskEntity),
                0,
                2,
                MARK,
                true,
                false,
                DAILY_CRON,
                LocalDateTime.MIN);

        assertNode(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.mergeNode(taskNode);

        assertNode(mergedTaskLink, taskNode);
    }

    // TODO: Routine tests
}
