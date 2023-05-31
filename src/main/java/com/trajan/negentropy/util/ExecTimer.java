package com.trajan.negentropy.util;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.util.StopWatch;

@SpringComponent
@UIScope
public class ExecTimer extends StopWatch {

    public ExecTimer() {
        super();
        mark("ExecTimer created");
    }

    public void mark(String taskName) {
        if(isRunning()) {
            stop();
        }
        start(taskName);
    }

    public String print() {
        stop();
        return prettyPrint();
    }
}
