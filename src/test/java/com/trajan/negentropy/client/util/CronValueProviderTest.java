package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.K;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: Proper UI cron validation
@SpringBootTest
class CronValueProviderTest {

    @Test
    void testValidCronExpressions() {
        assertTrue("0 0 * * * *".matches(K.CRON_PATTERN));
        assertTrue("*/10 * * * * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 8-10 * * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 6,19 * * *".matches(K.CRON_PATTERN));
        assertTrue("0 0/30 8-10 * * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 9-17 * * MON-FRI".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 25 12 ?".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 L * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 L-3 * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 1W * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 LW * *".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 * * 5L".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 * * THUL".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 ? * 5#2".matches(K.CRON_PATTERN));
        assertTrue("0 0 0 ? * MON#1".matches(K.CRON_PATTERN));
    }

    @Test
    void testInvalidCronExpression() {
        String invalid = "This is not a valid cron";

        assertFalse(invalid.matches(K.CRON_PATTERN));
    }
}