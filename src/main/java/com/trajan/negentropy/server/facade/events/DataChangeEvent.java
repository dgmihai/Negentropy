package com.trajan.negentropy.server.facade.events;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

@Accessors(fluent = true)
@Getter
public abstract class DataChangeEvent extends ApplicationEvent {
// TODO: Currently unused
    public DataChangeEvent(Object source) {
        super(source);
    }

    @Accessors(fluent = true)
    @Getter
    public static class Create<T> extends DataChangeEvent {
        private final T created;

        public Create(Object source, T created) {
            super(source);
            this.created = created;
        }
    }

    @Accessors(fluent = true)
    @Getter
    public static class Update<T1, T2> extends DataChangeEvent {
        private final T1 original;
        private final T2 updated;

        public Update(Object source, T1 original, T2 updated) {
            super(source);
            this.original = original;
            this.updated = updated;
        }
    }

    @Accessors(fluent = true)
    @Getter
    public static class Delete<T> extends DataChangeEvent {
        private final T deleted;

        public Delete(Object source, T deleted) {
            super(source);
            this.deleted = deleted;
        }
    }
}
