package com.trajan.negentropy.server.backend.netduration;

import com.trajan.negentropy.util.ServerClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RoutineLimiterTest {
    @Autowired private ServerClockService clock;
    static final Duration ONE_SECOND = Duration.ofSeconds(1);
    static final LocalDateTime ZERO_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @BeforeEach
    void setup() {
        clock.manualTime(ZERO_TIME);
    }

    @Test
    void testSmoke() {
        RoutineLimiter limiter = new RoutineLimiter(
                null,
                null,
                null,
                null,
                false);

        assertTrue(limiter.isEmptyWithEffort());

        assertFalse(limiter.wouldExceed(ONE_SECOND));
        limiter.include(ONE_SECOND, false);
        assertFalse(limiter.exceeded());

        assertTrue(limiter.isEmptyWithEffort());
    }

    void limiterMinuteTest(RoutineLimiter limiter) {
        assertFalse(limiter.isEmptyWithEffort());
        assertFalse(limiter.wouldExceed(ONE_SECOND));
        assertFalse(limiter.exceeded());

        assertTrue(limiter.wouldExceed(Duration.ofHours(1)));
        assertFalse(limiter.wouldExceed(ONE_SECOND));
        assertFalse(limiter.exceeded());

        limiter.include(ONE_SECOND, false);
        assertFalse(limiter.exceeded());

        assertTrue(limiter.wouldExceed(Duration.ofMinutes(1)));
        assertFalse(limiter.exceeded());

        limiter.include(Duration.ofMinutes(1), false);
        assertTrue(limiter.exceeded());
        assertTrue(limiter.wouldExceed(ONE_SECOND));

        limiter.include(Duration.ofMinutes(1), false);
        assertTrue(limiter.exceeded());
        assertTrue(limiter.wouldExceed(ONE_SECOND));

        limiter.include(Duration.ofDays(1), false);
        assertTrue(limiter.exceeded());
        assertTrue(limiter.wouldExceed(ONE_SECOND));
    }

    @Test
    void testEtaLimit() {
        limiterMinuteTest(new RoutineLimiter(
                null,
                null,
                ZERO_TIME.plusMinutes(1),
                null,
                false));
    }

    @Test
    void testDurationLimit() {
        limiterMinuteTest(new RoutineLimiter(
                Duration.ofMinutes(1),
                null,
                null,
                null,
                false));
    }

    @Test
    void testStepCountLimit() {
        RoutineLimiter limiter = new RoutineLimiter(
                null,
                3,
                null,
                null,
                false);

        assertFalse(limiter.isEmptyWithEffort());

        for (int i = 0; i < 3; i++) {
            assertFalse(limiter.wouldExceed(ONE_SECOND));
            limiter.include(ONE_SECOND, true);
            assertFalse(limiter.exceeded());
        }

        assertFalse(limiter.wouldExceed(ONE_SECOND));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());

        for (int i = 0; i < 3; i++) {
            assertFalse(limiter.wouldExceed(ONE_SECOND));
            limiter.include(ONE_SECOND, false);
            assertFalse(limiter.exceeded());
        }

        assertTrue(limiter.wouldExceed(ONE_SECOND));
        limiter.include(ONE_SECOND, false);
        assertTrue(limiter.exceeded());
    }

    @Test
    void testEffortLimit() {
        RoutineLimiter limiter = new RoutineLimiter(
                null,
                null,
                null,
                3,
                false);

        assertFalse(limiter.isEmptyWithEffort());
        assertTrue(limiter.isEmptyWithoutEffort());


        assertFalse(limiter.wouldExceed(ONE_SECOND, null));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());

        assertFalse(limiter.wouldExceed(ONE_SECOND, 1));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());

        assertFalse(limiter.wouldExceed(ONE_SECOND, 2));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());

        assertFalse(limiter.wouldExceed(ONE_SECOND, 3));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());

        assertTrue(limiter.wouldExceed(ONE_SECOND, 4));
        limiter.include(ONE_SECOND, true);
        assertFalse(limiter.exceeded());
    }

    @Test
    void testCombinationLimit() {
        limiterMinuteTest(new RoutineLimiter(
                Duration.ofMinutes(1),
                null,
                ZERO_TIME.plusMinutes(1),
                null,
                false));
    }
}