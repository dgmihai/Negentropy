package com.trajan.negentropy.model.sync;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.filter.TaskFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change.CopyChange.CopyType;
import lombok.Getter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
public abstract class Change {
    private final int id = hashCode();

    // Base change types

    public static <DATA extends Data> PersistChange<DATA> persist(DATA data) {
        return new PersistChange<>(data);
    }

    /*
     * Persists a new Task, TaskNode, or Tag that does not exist in the database.
     */
    @Getter
    public static class PersistChange<DATA extends Data> extends Change implements ChangeRecord<DATA> {
        protected final DATA data;

        private PersistChange(DATA data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "PersistChange(" + data + ")";
        }
    }

    public static <DATA extends PersistedDataDO<?>> MergeChange<DATA> merge(DATA data) {
        return new MergeChange<>(data);
    }

    /*
     * Merges changes to an existing Task, TaskNode, or Tag.
     */
    @Getter
    public static class MergeChange<DATA extends PersistedDataDO<? extends ID>> extends Change implements ChangeRecord<DATA> {
        private final DATA data;

        private MergeChange(DATA data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "MergeChange(" + data + ")";
        }
    }

    public static Change update(Task task) {
        return task.id() != null
                ? merge(task)
                : persist(task);
    }

    public static <UID extends ID> DeleteChange<UID> delete(UID id) {
        return new DeleteChange<>(id);
    }

    /*
     * Deletes an existing Task, TaskNode, or Tag.
     */
    @Getter
    public static class DeleteChange<UID extends ID> extends Change  implements ChangeRecord<UID> {
        private final UID data;

        public DeleteChange(UID data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "DeleteChange(" + data + ")";
        }
    }

    // Additional change types

    /*
     * Adds an existing Task to a new location in the Task tree based on a LinkID reference.
     */
    public static InsertChange insert(TaskNodeDTO nodeDTO, LinkID reference, InsertLocation location) {
        return new InsertChange(nodeDTO, reference, location);
    }

    @Getter
    public static class InsertChange extends Change {
        protected final TaskNodeDTO nodeDTO;
        protected final MultiValueMap<LinkID, InsertLocation> locations;

        private InsertChange(TaskNodeDTO nodeDTO, LinkID reference, InsertLocation location) {
            this.nodeDTO = nodeDTO;
            this.locations = new LinkedMultiValueMap<>();
            locations.add(reference, location);
        }

        private InsertChange(TaskNodeDTO nodeDTO, MultiValueMap<LinkID, InsertLocation> locations) {
            this.nodeDTO = nodeDTO;
            this.locations = locations;
        }

        @Override
        public String toString() {
            return "InsertChange(" + nodeDTO + " " + locations + ")";
        }
    }

    public static ReferencedInsertChange referencedInsert(TaskNodeDTO nodeDTO, LinkID reference, InsertLocation location, int changeTaskReference) {
        return new ReferencedInsertChange(nodeDTO, reference, location, changeTaskReference);
    }

    /*
     * Persists a new TaskNode that does not exist in the database.
     * References another change that is expected to return a Task.
     */
    @Getter
    public static class ReferencedInsertChange extends InsertChange {
        private final int changeTaskReference;

        private ReferencedInsertChange(TaskNodeDTO nodeDTO, MultiValueMap<LinkID, InsertLocation> locations, int changeTaskReference) {
            super(nodeDTO, locations);
            this.changeTaskReference = changeTaskReference;
        }

        private ReferencedInsertChange(TaskNodeDTO nodeDTO, LinkID reference, InsertLocation location, int changeTaskReference) {
            super(nodeDTO, reference, location);
            this.changeTaskReference = changeTaskReference;
        }

        @Override
        public String toString() {
            return "ReferencedInsertChange(" + nodeDTO + " " + locations + ")";
        }
    }

    public static InsertIntoChange insertInto(TaskNodeDTO nodeDTO, TaskID reference, InsertLocation location) {
        return new InsertIntoChange(nodeDTO, reference, location);
    }

    /*
     * Adds an existing Task to a new location in the Task tree based on a Task ID reference.
     */
    @Getter
    public static class InsertIntoChange extends Change {
        private final TaskNodeDTO nodeDTO;
        private final MultiValueMap<TaskID, InsertLocation> locations;

        private InsertIntoChange(TaskNodeDTO nodeDTO, TaskID reference, InsertLocation location) {
            this.nodeDTO = nodeDTO;
            this.locations = new LinkedMultiValueMap<>();
            locations.add(reference, location);
        }

        private InsertIntoChange(TaskNodeDTO nodeDTO, MultiValueMap<TaskID, InsertLocation> locations) {
            this.nodeDTO = nodeDTO;
            this.locations = locations;
        }

        @Override
        public String toString() {
            return "InsertIntoChange(" + nodeDTO + " " + locations + ")";
        }
    }

    public static MoveChange move(LinkID linkId, LinkID reference, InsertLocation location) {
        return new MoveChange(linkId, reference, location);
    }

    /*
     * Moves an existing TaskLink to additional locations in the Task tree, and removes it from its previous location.
     */
    @Getter
    public static class MoveChange extends Change {
        private final LinkID originalId;
        private final MultiValueMap<LinkID, InsertLocation> locations;

        private MoveChange(LinkID originalId, LinkID reference, InsertLocation location) {
            this.originalId = originalId;
            this.locations = new LinkedMultiValueMap<>();
            locations.add(reference, location);
        }

        @Override
        public String toString() {
            return "MoveChange(" + originalId + " " + locations + ")";
        }
    }

    public static <TEMPLATE extends Data, UID extends ID> MultiMergeChange<TEMPLATE, UID> multiMerge(TEMPLATE template, UID... ids) {
        return new MultiMergeChange<>(template, List.of(ids));
    }

    public static <TEMPLATE extends Data, UID extends ID> MultiMergeChange<TEMPLATE, UID> multiMerge(TEMPLATE template, Collection<UID> ids) {
        return new MultiMergeChange<>(template, ids);
    }

    /*
     * Merges a template into multiple existing Tasks, TaskNodes, or Tags.
     */
    @Getter
    public static class MultiMergeChange<TEMPLATE extends Data, UID extends ID> extends Change {
        private final TEMPLATE template;
        private final Collection<UID> ids;

        public MultiMergeChange(TEMPLATE template, Collection<UID> ids) {
            this.template = template;
            this.ids = ids;
        }

        @Override
        public String toString() {
            return "MergeChange(" + template + ", " + ids + ")";
        }
    }

    public static CopyChange shallowCopy(LinkID linkId, LinkID reference, InsertLocation location, TaskFilter taskFilter, String suffix) {
        return new CopyChange(linkId, reference, location, CopyType.SHALLOW, taskFilter, suffix);
    }

    public static CopyChange deepCopy(LinkID linkId, LinkID reference, InsertLocation location, TaskFilter taskFilter, String suffix) {
        return new CopyChange(linkId, reference, location, CopyType.DEEP, taskFilter, suffix);
    }

    /*
     * Copies an existing TaskNode and its children (after filter) to additional locations in the Task tree.
     * Can either be shallow (descendants refer to same tasks as previous) or deep (all tasks are new and separate instances).
     */
    @Getter
    public static class CopyChange extends Change {
        protected final LinkID originalId;
        protected final MultiValueMap<LinkID, InsertLocation> locations;
        protected final TaskFilter taskFilter;
        protected final String suffix;
        protected final CopyType copyType;

        public enum CopyType {
            SHALLOW,
            DEEP
        }

        private CopyChange(LinkID originalId, LinkID reference, InsertLocation location, CopyType copyType, TaskFilter taskFilter, String suffix) {
            this.originalId = originalId;
            this.locations = new LinkedMultiValueMap<>();
            this.locations.add(reference, location);
            this.taskFilter = taskFilter;
            this.suffix = suffix;
            this.copyType = copyType;
        }
    }

    public static OverrideScheduledForChange setScheduledFor(LinkID linkId, LocalDateTime overrideScheduledFor) {
        return new OverrideScheduledForChange(linkId, overrideScheduledFor);
    }

    /*
     * Merges a template into multiple existing Tasks, TaskNodes, or Tags.
     */
    @Getter
    public static class OverrideScheduledForChange extends Change {
        private final LinkID linkId;
        private final LocalDateTime manualScheduledFor;

        public OverrideScheduledForChange(LinkID linkId, LocalDateTime manualScheduledFor) {
            this.linkId = linkId;
            this.manualScheduledFor = manualScheduledFor;
        }

        @Override
        public String toString() {
            return "OverrideScheduledForChange(" + linkId + ", " + manualScheduledFor + ")";
        }
    }
}
