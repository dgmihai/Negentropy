package com.trajan.negentropy.client.controller.event;

import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.TaskInfo;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

@UIScope
@Getter
public abstract class TaskInfoFormEvent extends ApplicationEvent {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfoFormEvent.class);

    protected TaskInfo data;
    protected TaskInfoForm source;

    public TaskInfoFormEvent(TaskInfoForm source, TaskInfo data) {
        super(source);
        this.data = data;
    }

    public static class SaveEvent extends TaskInfoFormEvent {
        public SaveEvent(TaskInfoForm source, TaskInfo data) {
            super(source, data);
            logger.debug("== {} ==", this.getClass());
        }
    }

    public static class DeleteEvent extends TaskInfoFormEvent {
        public DeleteEvent(TaskInfoForm source, TaskInfo data) {
            super(source, data);
            logger.debug("== {} ==", this.getClass());
        }
    }

    public static class ClearEvent extends TaskInfoFormEvent {
        public ClearEvent(TaskInfoForm source) {
            super(source, null);
            logger.debug("== {} ==", this.getClass());
        }
    }
}