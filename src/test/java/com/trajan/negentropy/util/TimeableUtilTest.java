package com.trajan.negentropy.util;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.entity.routine.RoutineStep.RoutineTaskStep;
import com.trajan.negentropy.server.backend.util.DFSUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class TimeableUtilTest {
    private static TestTimeable root;
    private static Map<String, RoutineStep> timeables;
    private static LocalDateTime NOW;

    private final TimeableUtil timeableUtil = new TimeableUtil() {
        @Override
        public LocalDateTime currentTime() {
            return LocalDateTime.MIN;
        }
    };

    @Getter
    @Setter
    @Accessors(fluent = true)
    static class TestTimeable extends RoutineTaskStep {
        public TestTimeable(Task task) {
            super(task);
        }
    }

    private static TestTimeable createTimeable(String name) {
        TestTimeable timeable = new TestTimeable(new Task(null)
                .name(name)
                .description(name)
                .duration(Duration.ofMinutes(10)));
        timeable.children(new ArrayList<>());
        timeable.status(TimeableStatus.NOT_STARTED);
        return timeable;
    }

    @BeforeAll
    public static void setUp() {
        root = createTimeable("root");
        root.children().addAll(List.of(
                createTimeable("A"),
                createTimeable("B"),
                createTimeable("C"),
                createTimeable("D")));
        root.children().get(1).children().addAll(List.of(
                createTimeable("W"),
                createTimeable("X"),
                createTimeable("Y"),
                createTimeable("Z")));

        timeables = DFSUtil.traverse(root).stream().collect(Collectors.toMap(
                RoutineStep::name,
                timeable -> timeable));

        timeables.get("root").status(TimeableStatus.ACTIVE)
                .startTime(LocalDateTime.MIN);
        timeables.get("A").status(TimeableStatus.EXCLUDED)
                .startTime(LocalDateTime.MIN)
                .finishTime(LocalDateTime.MIN.plusMinutes(10));
        timeables.get("B").status(TimeableStatus.ACTIVE)
                .startTime(LocalDateTime.MIN.plusMinutes(10));
        timeables.get("W").status(TimeableStatus.SKIPPED);
        timeables.get("X").status(TimeableStatus.COMPLETED)
                .startTime(LocalDateTime.MIN.plusMinutes(10))
                .finishTime(LocalDateTime.MIN.plusMinutes(30));
        timeables.get("Y").status(TimeableStatus.COMPLETED)
                .startTime(LocalDateTime.MIN.plusMinutes(30))
                .finishTime(LocalDateTime.MIN.plusMinutes(35));
        timeables.get("Z").status(TimeableStatus.SUSPENDED)
                .startTime(LocalDateTime.MIN.plusMinutes(35))
                .lastSuspendedTime(LocalDateTime.MIN.plusMinutes(50));
        timeables.get("D").status(TimeableStatus.ACTIVE)
                .startTime(LocalDateTime.MIN)
                .lastSuspendedTime(LocalDateTime.MIN.plusMinutes(5))
                .elapsedSuspendedDuration(Duration.ofMinutes(45));

        NOW = LocalDateTime.MIN.plusMinutes(50);
    }

    @Test
    public void testGetElapsedActiveDuration() {
        assertEquals(Duration.ofMinutes(50),
                timeableUtil.getElapsedActiveDuration(timeables.get("root"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getElapsedActiveDuration(timeables.get("A"), NOW));

        assertEquals(Duration.ofMinutes(40),
                timeableUtil.getElapsedActiveDuration(timeables.get("B"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getElapsedActiveDuration(timeables.get("W"), NOW));

        assertEquals(Duration.ofMinutes(20),
                timeableUtil.getElapsedActiveDuration(timeables.get("X"), NOW));

        assertEquals(Duration.ofMinutes(5),
                timeableUtil.getElapsedActiveDuration(timeables.get("Y"), NOW));

        assertEquals(Duration.ofMinutes(15),
                timeableUtil.getElapsedActiveDuration(timeables.get("Z"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getElapsedActiveDuration(timeables.get("C"), NOW));

        assertEquals(Duration.ofMinutes(5),
                timeableUtil.getElapsedActiveDuration(timeables.get("D"), NOW));
    }

    @Test
    public void testGetRemainingDuration() {
        assertEquals(Duration.ofMinutes(40).negated(),
                timeableUtil.getRemainingDuration(timeables.get("root"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingDuration(timeables.get("A"), NOW));

        assertEquals(Duration.ofMinutes(30).negated(),
                timeableUtil.getRemainingDuration(timeables.get("B"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getRemainingDuration(timeables.get("W"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingDuration(timeables.get("X"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingDuration(timeables.get("Y"), NOW));

        assertEquals(Duration.ofMinutes(5).negated(),
                timeableUtil.getRemainingDuration(timeables.get("Z"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getRemainingDuration(timeables.get("C"), NOW));

        assertEquals(Duration.ofMinutes(5),
                timeableUtil.getRemainingDuration(timeables.get("D"), NOW));

    }

    @Test
    public void testGetRemainingNetDuration() {
        assertEquals(Duration.ofMinutes(25),
                timeableUtil.getRemainingNetDuration(timeables.get("root"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingNetDuration(timeables.get("A"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getRemainingNetDuration(timeables.get("B"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getRemainingNetDuration(timeables.get("W"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingNetDuration(timeables.get("X"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingNetDuration(timeables.get("Y"), NOW));

        assertEquals(Duration.ZERO,
                timeableUtil.getRemainingNetDuration(timeables.get("Z"), NOW));

        assertEquals(Duration.ofMinutes(10),
                timeableUtil.getRemainingNetDuration(timeables.get("C"), NOW));

        assertEquals(Duration.ofMinutes(5),
                timeableUtil.getRemainingNetDuration(timeables.get("D"), NOW));
    }
}