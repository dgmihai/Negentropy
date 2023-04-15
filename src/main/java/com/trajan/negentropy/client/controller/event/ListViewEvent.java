package com.trajan.negentropy.client.controller.event;

import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@UIScope
@Getter
public abstract class ListViewEvent extends ApplicationEvent {
    private static final Logger logger = LoggerFactory.getLogger(ListViewEvent.class);

    protected TaskNode data;
    protected ListView source;

    public ListViewEvent(ListView source, TaskNode data) {
        super(source);
        this.source = source;
        this.data = data;
    }

    public static class SaveEvent extends ListViewEvent {
        public SaveEvent(ListView source, TaskNode data) {
            super(source, data);
            logger.debug("== {} ==", this.getClass());
        }
    }

    public static class RemoveEvent extends ListViewEvent {
        public RemoveEvent(ListView source, TaskNode data) {
            super(source, data);
            logger.debug("== {} ==", this.getClass());
        }
    }

    public static class UpdateEvent extends ListViewEvent {
        public UpdateEvent(ListView source, List<Filter> filters) {
            super(source, null);
            logger.debug("== {} ==", this.getClass());
        }
    }
}