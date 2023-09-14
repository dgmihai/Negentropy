package com.trajan.negentropy.model.sync;

import com.trajan.negentropy.model.id.LinkID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public abstract class AbstractChangelist <T> {
    @NonNull
    protected UUID syncId = UUID.randomUUID();
    protected final List<T> changes = new ArrayList<>();
    protected LocalDateTime timestamp = LocalDateTime.now();

    public static class Changelist extends AbstractChangelist<Change> {
        public static Changelist of(Change... changes) {
            Changelist changelist = new Changelist();
            changelist.changes.addAll(Arrays.asList(changes));
            return changelist;
        }
    }

    @Getter
    public static class SyncChangelist extends AbstractChangelist<ChangeRecord<?>> {
        private final Map<LinkID, Duration> netDurationMap = new HashMap<>();

        public static SyncChangelist of(ChangeRecord<?>... changes) {
            SyncChangelist changelist = new SyncChangelist();
            changelist.changes.addAll(Arrays.asList(changes));
            return changelist;
        }
    }
}