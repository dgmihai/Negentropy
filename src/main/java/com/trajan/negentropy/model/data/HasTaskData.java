package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.interfaces.Named;

import java.time.Duration;
import java.util.Set;

public interface HasTaskData {
    Task task();

    interface TaskTemplateData<TASK extends TaskTemplateData<TASK>>
            extends Data {
        TASK description(String description);
        String description();
        TASK duration(Duration duration);
        Duration duration();
        TASK required(Boolean required);
        Boolean required();
        TASK project(Boolean project);
        Boolean project();
        TASK difficult(Boolean difficult);
        Boolean difficult();
    }

    interface TaskTemplateDataWithTags<TASK extends TaskTemplateDataWithTags<TASK, TAG>, TAG extends TagData<TAG>>
            extends TaskTemplateData<TASK> {
        TASK tags(Set<TAG> tags);
        Set<TAG> tags();
    }

    interface TaskData<TASK extends TaskData<TASK>>
            extends TaskTemplateData<TASK>, Named {
        TASK name(String name);

        default String typeName() {
            return project() != null
                    ? project() ? "Project" : "Task"
                    : "Task";
        }
    }
}
