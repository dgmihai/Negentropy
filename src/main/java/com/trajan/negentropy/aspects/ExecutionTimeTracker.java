package com.trajan.negentropy.aspects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

@Slf4j
public class ExecutionTimeTracker {
    private static final StopWatch stopWatch = new StopWatch();

    public static void mark() {
        String methodName = new Object() {}
                .getClass()
                .getEnclosingMethod()
                .getName();
        if (stopWatch.isRunning()) {
            stopWatch.stop();
        }

        stopWatch.start(methodName);
    }

    public static void prettyPrint() {
        log.debug(stopWatch.prettyPrint());
    }
}