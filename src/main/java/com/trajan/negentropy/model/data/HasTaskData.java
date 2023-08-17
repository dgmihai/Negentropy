package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.interfaces.Named;

import java.time.Duration;
import java.util.Set;

public interface HasTaskData {
    Task task();

    interface TaskTemplateData<TASK extends TaskTemplateData<TASK, TAG>, TAG extends TagData<TAG>>
            extends Data {
        TASK description(String description);
        String description();
        TASK duration(Duration duration);
        Duration duration();
        TASK required(Boolean required);
        Boolean required();
        TASK project(Boolean project);
        Boolean project();
        TASK tags(Set<TAG> tags);
        Set<TAG> tags();
    }

    interface TaskData<TASK extends TaskData<TASK, TAG>, TAG extends TagData<TAG>>
            extends TaskTemplateData<TASK, TAG>, Named {
        TASK name(String name);
    }
}
