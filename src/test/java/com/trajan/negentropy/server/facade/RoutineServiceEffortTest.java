package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.filter.RoutineLimitFilter;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.RoutineTestTemplateWithEffort;
import com.trajan.negentropy.server.facade.response.Request;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class RoutineServiceEffortTest extends RoutineTestTemplateWithEffort {

    @BeforeAll
    void setup() {
        init();
    }

    private void linkRoutineCreationTestWithEffort(
            Triple<String, String, Integer> rootLink,
            Function<TaskID, Duration> expectedDuration,
            Integer effort,
            List<String> expectedSteps,
            List<String> excludedSteps) {
        linkRoutineCreationTestWithExpectedDurationAndFilter(
                rootLink,
                expectedDuration,
                new RoutineLimitFilter()
                        .effortMaximum(effort),
                expectedSteps,
                excludedSteps);
    }

    @Test
    void testCreateRoutineFromProjectLinkWithEffortFilter_ExcludeCompleted() {
        TaskNode twoTwo = nodes.get(Triple.of(TWO, TWOTWO, 1));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo
                        .completed(true)
                        .recurring(false))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(4),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(4),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTHREE),
                List.of());

        changeService.execute(Request.of(new MergeChange<>(
                twoTwo
                        .completed(false))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());
    }

    @Test
    void testCreateRoutineFromProjectLinkWithEffortFilter() {
        TaskNode node = nodes.get(Triple.of(NULL, TWO, 1));

        changeService.execute(Request.of(new MergeChange<>(
                node.projectDurationLimit(Optional.empty()))));

        linkRoutineCreationTestWithExpectedDurationAndFilter(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                null,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                4,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(7),
                3,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        TWOTHREE),
                List.of());

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(6),
                2,
                List.of(
                        TWO,
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(
                        TWOTWOTHREE_AND_THREETWOTWO));

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(5),
                1,
                List.of(
                        TWO,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTHREE),
                List.of(
                        TWOONE,
                        TWOTWOTHREE_AND_THREETWOTWO));

        linkRoutineCreationTestWithEffort(
                Triple.of(NULL, TWO, 1),
                id -> Duration.ofHours(3),
                0,
                List.of(
                        TWO,
                        TWOTHREE),
                List.of(
                        TWOONE,
                        TWOTWO,
                        TWOTWOONE,
                        TWOTWOTWO,
                        TWOTWOTHREE_AND_THREETWOTWO));
    }
}

