package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.MultiMergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.RoutineTestTemplateWithRequiredTasks;
import com.trajan.negentropy.server.facade.response.Request;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceWithRequiredTest extends RoutineTestTemplateWithRequiredTasks {

    @BeforeAll
    void setup() {
        init();
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitedProject() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                id -> Duration.ofHours(2),
                List.of(
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTWOONE,
                        TWOTWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingStepCount() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(5)))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .stepCountLimit(3);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of());

        filter = new RoutineLimitFilter()
                .stepCountLimit(2);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                filter,
                List.of(
                        TWO,
                        TWOONE),
                List.of());


        filter = new RoutineLimitFilter()
                .stepCountLimit(99);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingEta() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(5)))));

        RoutineLimitFilter filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(Duration.ofHours(6)));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of());

        filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.now().plus(Duration.ofHours(3)));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                filter,
                List.of(
                        TWO,
                        TWOONE),
                List.of());

        filter = new RoutineLimitFilter()
                .etaLimit(LocalDateTime.MAX);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_2() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(3);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(3)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE),
                List.of(
                        TWOTWO,
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_3() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Duration.ofHours(5)))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of(
                        TWOTHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_TWO_4() throws Exception {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectTaskWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

        RoutineLimitFilter filter = new RoutineLimitFilter();
        filter.durationLimit(routineDuration);

        taskRoutineCreationTestWithExpectedDurationAndFilter(
                TWO,
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithManualDurationLimit_TWO() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(5);

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(routineDuration))));

        RoutineLimitFilter filter = new RoutineLimitFilter();
        filter.durationLimit(routineDuration);

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                filter,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectTaskWithNonLimitingProjectDuration_TWO() throws Exception {
        TaskNode two = nodes.get(Triple.of(NULL, TWO, 1));
        Duration routineDuration = Duration.ofHours(6);

        changeService.execute(Request.of(
                new MergeChange<>(
                        two.projectDurationLimit(routineDuration))));

        two = queryService.fetchNode(two.id());
        assertEquals(routineDuration, two.projectDurationLimit());

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, TWO, 1),
                id -> routineDuration,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                id -> Duration.ofMinutes(13*60 + 30),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        SIX_AND_THREETWOFOUR),
                List.of(
                        THREETHREE));
    }

    @Test
    void testCreateRoutineFromDifferentProjectLinkWithLimitingProjectDuration_THREE_AND_FIVE() throws Exception {
        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(NULL, THREE_AND_FIVE, 4),
                id -> Duration.ofMinutes(210),
                List.of(
                        THREE_AND_FIVE,
                        THREEONE),
                List.of(
                        THREETWO,
                        THREETHREE));
    }

    @Test
    void testCreateRoutineFromProjectLinkWithNestedLimitedProjectWithTagFilter() throws Exception {
        Change newTagChange = new PersistChange<>(new Tag().name("TestTag"));
        Tag newTag = (Tag) changeService.execute(Request.of(newTagChange)).changeRelevantDataMap()
                .get(newTagChange.id()).get(0);

        List<TaskID> targetTasks = Stream.of(tasks.get(TWOTWO), tasks.get(TWOTWOONE))
                .map(Task::id)
                .toList();

        Change multiTagChange = new MultiMergeChange<>(new Task()
                .tags(Set.of(newTag)),
                targetTasks);

        changeService.execute(Request.of(multiTagChange));

        linkRoutineCreationTestWithExpectedDuration(
                Triple.of(TWO, TWOTWO, 1),
                new TaskNodeTreeFilter()
                        .includedTagIds(Set.of(newTag.id())),
                id -> Duration.ofMinutes(90),
                List.of(
                        TWOTWO,
                        TWOTWOONE),
                List.of());
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersistAtBack() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id()))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREEONE),
                routine);
        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithPersistAtFront() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        changeService.execute(Request.of(new PersistChange<>(
                new TaskNodeDTO()
                        .parentId(tasks.get(TWOTWO).id())
                        .childId(tasks.get(THREEONE).id())
                        .position(0))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        THREEONE,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);
        assertEquals(routine.estimatedDuration(), originalDuration.plus(Duration.ofMinutes(30)));
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithDelete() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();

        changeService.execute(Request.of(new DeleteChange<>(
                ID.of(links.get(Triple.of(TWOTWO, TWOTWOONE, 0))))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(originalDuration.minus(Duration.ofMinutes(30)), routine.estimatedDuration());
    }

    @Test
    @Transactional
    void testRoutineRecalculateWithMerge() {
        Routine routine = routineService.createRoutine(tasks.get(TWOTWO).id()).routine();

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        Duration originalDuration = routine.estimatedDuration();
        assertEquals(Duration.ofHours(3), originalDuration);

        Task twoTwoOne = tasks.get(TWOTWOONE);
        changeService.execute(Request.of(new MergeChange<>(
                new Task(twoTwoOne.id())
                        .duration(twoTwoOne.duration().plus(Duration.ofHours(1))))));

        routine = routineService.fetchRoutine(routine.id());

        assertRoutine(List.of(
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO),
                routine);

        assertEquals(Duration.ofHours(4), routine.estimatedDuration());
    }
}
