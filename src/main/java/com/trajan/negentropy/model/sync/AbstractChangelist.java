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

//    public <T extends Data> Set<T> toSet(Class<T> clazz) {
//        Set<T> results = new HashSet<>();
//        for (Change change : changes) {
//            Data changes = null;
//            if (change instanceof Change.PersistChange<?> persist) {
//                changes = persist.changes();
//            } else if (change instanceof Change.MergeChange<?> merge) {
//                changes = merge.changes();
//            }
//
//            if (clazz.isInstance(changes)) {
//                results.add(clazz.cast(changes));
//            }
//        }
//        return results;
//    }
//
//    public Map<ID, PersistedDataDO<? extends ID>> toMap() {
//        Map<ID, PersistedDataDO<? extends ID>> results = new HashMap<>();
//        for (Change change : changes) {
//            PersistedDataDO<?> changes = null;
//            if (change instanceof Change.PersistChange<?> persist) {
//                changes = (PersistedDataDO<?>) persist.changes();
//            } else if (change instanceof Change.MergeChange<?> merge) {
//                changes = merge.changes();
//
//            }
//
//            results.put(changes.id(), changes);
//        }
//        return results;
//    }

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