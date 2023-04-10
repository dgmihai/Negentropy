package com.trajan.negentropy.view.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class StackLog {
    private static final Logger logger = LoggerFactory.getLogger(StackLog.class);

    public static void print(int additionalFrames) {
        List<StackWalker.StackFrame> frames = StackWalker.getInstance().walk(s -> s
                .filter(f -> f.getClassName().startsWith("com.trajan.negentropy") &&
                        !f.getClassName().equals("com.trajan.negentropy.view.util.StackLog"))
                .limit(additionalFrames)
                .collect(Collectors.toList()));
        for (StackWalker.StackFrame frame : frames) {
            logger.debug("    " + frame.toString());
        }
    }
}
