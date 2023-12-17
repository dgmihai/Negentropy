package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.helger.commons.mock.CommonsAssert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class DataContextTest extends TaskTestTemplate {
    @Autowired private DataContext dataContext;
    @Autowired private TagRepository tagRepository;

    private static final String DAILY_STRING = "@daily";
    private static final CronExpression DAILY_CRON = CronExpression.parse(DAILY_STRING);

    private void assertTask(TaskEntity taskEntity, Task task) {
        assertNotNull(task);
        assertNotNull(task.id());
        assertEquals(ID.of(taskEntity), task.id());
        assertEquals(taskEntity.name(), task.name());
        assertEquals(taskEntity.description(), task.description());
        assertEquals(taskEntity.duration(), task.duration());
        assertEquals(taskEntity.required(), task.required());
        assertEquals(taskEntity.project(), task.project());
    }

    @Test
    void testTaskEntityToDO() {
        TaskEntity taskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity()
                .name("Task Name")
                .description("Task Description")
                .duration(Duration.ofMinutes(120)))
                .required(true)
                .project(true)
                .difficult(true);

        Set<TagEntity> tagEntities = Set.of(
                dataContext.merge(new TagEntity().name("Tag1")),
                dataContext.merge(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(tagEntities);

        Task task = dataContext.toEagerDO(taskEntity);

        assertTask(taskEntity, task);
        assertEquals(tagEntities.size(), tagRepository.findByTasksId(task.id().val()).count());

        // TODO: Tags removed from Task DO due to N+1
//        task.tags().forEach(tag -> {
//            TagEntity originalTag = tagEntities.stream()
//                    .filter(tagEntity ->ID.of(tagEntity).equals(tag.id()))
//                    .findFirst()
//                    .orElse(null);
//            assertNotNull(originalTag);
//            assertEquals(originalTag.name(), tag.name());
//        });
    }

    @Test
    void testTagEntityToDO() {
        TagEntity tagEntity = dataContext.merge(new TagEntity().name("Tag Name"));
        Tag tag = dataContext.toDO(tagEntity);

        assertNotNull(tag);
        assertEquals(ID.of(tagEntity), tag.id());
        assertEquals(tagEntity.name(), tag.name());
    }

    private void assertNodeWithoutLimits(TaskLink taskLink, TaskNode taskNode) {
        assertNotNull(taskNode);
        assertEquals(ID.of(taskLink), taskNode.linkId());
        assertEquals(taskLink.importance(), taskNode.importance());
        assertEquals(taskLink.position(), taskNode.position());
        assertEquals(taskLink.positionFrozen(), taskNode.positionFrozen());
        assertEquals(taskLink.completed(), taskNode.completed());
        assertEquals(taskLink.recurring(), taskNode.recurring());
        assertEquals(taskLink.cycleToEnd(), taskNode.cycleToEnd());
        assertEquals(taskLink.cron(), taskNode.cron());
        assertEquals(taskLink.createdAt(), taskNode.createdAt());
        assertEquals(taskLink.scheduledFor(), taskNode.scheduledFor());
        assertEquals(ID.of(taskLink.parent()), taskNode.parentId());
        assertEquals(ID.of(taskLink.child()), taskNode.child().id());
    }

    private void assertNode(TaskLink taskLink, TaskNode taskNode) {
        assertNodeWithoutLimits(taskLink, taskNode);
        assertEquals(taskLink.projectDurationLimit().orElse(null), taskNode.projectDurationLimit().orElse(null));
        assertEquals(taskLink.projectStepCountLimit().orElse(null), taskNode.projectStepCountLimit().orElse(null));
        assertEquals(taskLink.projectEtaLimit().orElse(null), taskNode.projectEtaLimit().orElse(null));
    }

    @Test
    void testTaskLinkToDO() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink prevLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                null,
                parentTaskEntity,
                childTaskEntity,
                0,
                false,
                0,
                MARK,
                false,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN,
                Duration.ofMinutes(1),
                7,
                null));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                null,
                parentTaskEntity,
                childTaskEntity,
                2,
                false,
                1,
                MARK,
                false,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN,
                Duration.ofMinutes(1),
                7,
                null));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                null,
                parentTaskEntity,
                childTaskEntity,
                2,
                false,
                0,
                MARK,
                false,
                false,
                false,
                DAILY_STRING,
                LocalDateTime.MIN,
                Duration.ofMinutes(1),
                7,
                null));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = dataContext.toEagerDO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskLinkToDONoPrev() {
        TaskEntity parentTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Parent Task"));
        TaskEntity childTaskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity().name("Child Task"));

        TaskLink taskLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                null,
                parentTaskEntity,
                childTaskEntity,
                2,
                true,
                0,
                MARK,
                true,
                false,
                true,
                DAILY_STRING,
                LocalDateTime.MIN,
                Duration.ofMinutes(1),
                4,
                LocalTime.of(13, 0).toString()));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink(
                null,
                parentTaskEntity,
                childTaskEntity,
                1,
                true,
                0,
                MARK,
                true,
                false,
                true,
                DAILY_STRING,
                LocalDateTime.MIN,
                Duration.ofMinutes(1),
                4,
                LocalTime.of(13, 0).toString()));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = dataContext.toEagerDO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskLinkToDONoNext() {
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

        TaskNode taskNode = dataContext.toEagerDO(taskLink);

        assertNode(taskLink, taskNode);
    }

    @Test
    void testTaskMerge() {
        TaskEntity taskEntity = dataContext.TESTONLY_mergeTask(new TaskEntity()
                .name("Original Task Name")
                .description("Original Task Description")
                .duration(Duration.ofMinutes(120))
                .required(false)
                .project(true));

        Set<TagEntity> originalTagEntities = Set.of(
                dataContext.merge(new TagEntity().name("Tag1")),
                dataContext.merge(new TagEntity().name("Tag2"))
        );

        taskEntity.tags(originalTagEntities);

        Task task = new Task(ID.of(taskEntity),
                "Updated Task Name",
                "Updated Task Description",
                Duration.ofMinutes(180),
                false,
                true,
                false,
                false,
                originalTagEntities.stream().map(dataContext::toDO).collect(Collectors.toSet()));

        TaskEntity mergedTaskEntity = dataContext.merge(task);

        assertTask(mergedTaskEntity, task);
    }

    @Test
    void testTagMerge() {
        TagEntity tagEntity = dataContext.merge(new TagEntity().name("Original Tag Name"));
        Tag tag = new Tag(ID.of(tagEntity), "Updated Tag Name");

        TagEntity mergedTagEntity = dataContext.merge(tag);

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
                .completed(false)
                .position(1)
                .cron(DAILY_STRING)
                .scheduledFor(LocalDateTime.MIN)
                .projectDurationLimit(Optional.of(Duration.ofMinutes(1)))
                .projectStepCountLimit(Optional.of(3))
                .projectEtaLimit(Optional.of(LocalTime.of(12, 0))));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(2));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                dataContext.toEagerDO(childTaskEntity),
                1,
                false,
                2,
                MARK,
                false,
                false,
                false,
                DAILY_CRON,
                LocalDateTime.MIN,
                Optional.of(Duration.ofMinutes(1)),
                Optional.of(4),
                Optional.of(LocalTime.of(13, 0)));

        assertNodeWithoutLimits(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.merge(taskNode);

        assertNode(mergedTaskLink, taskNode);
    }

    @Test
    void testTaskLinkMergeNullProjectDurationLimits() {
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
                .completed(false)
                .position(1)
                .cron(DAILY_STRING)
                .scheduledFor(LocalDateTime.MIN)
                .projectDurationLimit(Optional.empty())
                .projectStepCountLimit(Optional.empty())
                .projectEtaLimit(Optional.empty()));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(2));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                dataContext.toEagerDO(childTaskEntity),
                1,
                false,
                2,
                MARK,
                false,
                false,
                false,
                DAILY_CRON,
                LocalDateTime.MIN,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertNode(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.merge(taskNode);

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

        assertThrows(Exception.class, () -> dataContext.merge(
                new TaskNodeDTO(
                        ID.of(parentTaskEntity),
                        ID.of(childTaskEntity),
                        4,
                        false,
                        2,
                        false,
                        false,
                        false,
                        DAILY_CRON,
                        Optional.of(Duration.ofMinutes(1)),
                        null,
                        null)));
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
                .cycleToEnd(true)
                .cron(DAILY_CRON)
                .scheduledFor(LocalDateTime.MIN)
                .projectDurationLimit(Optional.of(Duration.ofMinutes(1)))
                .projectStepCountLimit(Optional.of(5))
                .projectEtaLimit(LocalTime.of(11, 0)));

        parentTaskEntity.childLinks(List.of(prevLink, taskLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                dataContext.toEagerDO(childTaskEntity),
                1,
                false,
                2,
                MARK,
                true,
                false,
                true,
                DAILY_CRON,
                LocalDateTime.MIN,
                Optional.empty(),
                Optional.of(6),
                Optional.of(LocalTime.NOON));

        assertNodeWithoutLimits(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.merge(taskNode);
        assertTrue(mergedTaskLink.projectDurationLimit().isEmpty());
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
                .cycleToEnd(true)
                .cron(DAILY_CRON)
                .scheduledFor(LocalDateTime.MIN)
                .projectDurationLimit(Optional.of(Duration.ofMinutes(1)))
                .projectStepCountLimit(Optional.of(5))
                .projectEtaLimit(LocalTime.of(11, 0)));

        TaskLink nextLink = dataContext.TESTONLY_mergeLink(new TaskLink()
                .parent(parentTaskEntity)
                .child(childTaskEntity)
                .position(1));

        parentTaskEntity.childLinks(List.of(taskLink, nextLink));

        TaskNode taskNode = new TaskNode(
                ID.of(taskLink),
                ID.of(parentTaskEntity),
                dataContext.toEagerDO(childTaskEntity),
                0,
                false,
                2,
                MARK,
                true,
                false,
                true,
                DAILY_CRON,
                LocalDateTime.MIN,
                Optional.of(Duration.ofMinutes(1)),
                Optional.of(6),
                Optional.of(LocalTime.NOON));

        assertNodeWithoutLimits(taskLink, taskNode);

        TaskLink mergedTaskLink = dataContext.merge(taskNode);

        assertNode(mergedTaskLink, taskNode);
    }

    @Test
    void testRoutineToDO() {
        RoutineStepEntity stepThree = new RoutineStepEntity()
                .task(dataContext.TESTONLY_mergeTask(new TaskEntity().name("Three")));

        RoutineStepEntity stepTwoOne = new RoutineStepEntity()
                .task(dataContext.TESTONLY_mergeTask(new TaskEntity().name("TwoOne")));
        RoutineStepEntity stepTwoTwo = new RoutineStepEntity()
                .task(dataContext.TESTONLY_mergeTask(new TaskEntity().name("TwoTwo")))
                .children(List.of(stepThree));

        RoutineStepEntity stepOne = new RoutineStepEntity()
                .task(dataContext.TESTONLY_mergeTask(new TaskEntity().name("One")))
                .children(List.of(stepTwoOne, stepTwoTwo));

        RoutineEntity routineEntity = dataContext.TESTONLY_mergeRoutine(new RoutineEntity()
                .children(List.of(stepOne)));

        Routine routine = dataContext.toDO(routineEntity);

        assertEquals(1, routine.children().size());
        assertEquals("One", routine.children().get(0).task().name());
        assertEquals(2, routine.children().get(0).children().size());
        assertEquals("TwoOne", routine.children().get(0).children().get(0).task().name());
        assertEquals("TwoTwo", routine.children().get(0).children().get(1).task().name());
        assertEquals(1, routine.children().get(0).children().get(1).children().size());
        assertEquals("Three", routine.children().get(0).children().get(1).children().get(0).task().name());
        assertEquals(0, routine.children().get(0).children().get(0).children().size());
    }
}
