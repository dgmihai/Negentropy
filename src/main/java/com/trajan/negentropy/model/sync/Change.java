package com.trajan.negentropy.model.sync;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.id.TaskID;
import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.Collection;

@Getter
public abstract class Change {
    private final int id = hashCode();

    // Base change types

    /*
     * Persists a new Task, TaskNode, or Tag that does not exist in the database.
     */
    @Getter
    public static class PersistChange<DATA extends Data> extends Change implements ChangeRecord<DATA> {
        protected final DATA data;

        public PersistChange(DATA data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "PersistChange(" + data + ")";
        }
    }

    /*
     * Merges changes to an existing Task, TaskNode, or Tag.
     */
    @Getter
    public static class MergeChange<DATA extends PersistedDataDO<? extends ID>> extends Change implements ChangeRecord<DATA> {
        private final DATA data;

        public MergeChange(DATA data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "MergeChange(" + data + ")";
        }
    }

    public static Change update(Task task) {
        return task.id() != null
                ? new MergeChange<>(task)
                : new PersistChange<>(task);
    }

    /*
     * Deletes an existing Task, TaskNode, or Tag.
     */
    @Getter
    public static class DeleteChange<UID extends ID> extends Change implements ChangeRecord<UID> {
        private final UID data;

        public DeleteChange(UID data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "DeleteChange(" + data + ")";
        }
    }

    @Getter
    public abstract static class InsertChange<I extends ID> extends Change {
        protected final TaskNodeDTO nodeDTO;
        protected final MultiValueMap<I, InsertLocation> locations;

        public InsertChange(TaskNodeDTO nodeDTO,I reference, InsertLocation location) {
            this.nodeDTO = nodeDTO;
            this.locations = new LinkedMultiValueMap<>();
            locations.add(reference, location);
        }

        public InsertChange(TaskNodeDTO nodeDTO, MultiValueMap<I, InsertLocation> locations) {
            this.nodeDTO = nodeDTO;
            this.locations = locations;
        }
    }

    // Additional change types

    /*
     * Adds an existing Task to a new location in the Task tree based on a LinkID reference.
     */
    @Getter
    public static class InsertAtChange extends InsertChange<LinkID> {
        public InsertAtChange(TaskNodeDTO nodeDTO, @NonNull LinkID reference, InsertLocation location) {
            super(nodeDTO, reference, location);
        }

        public InsertAtChange(TaskNodeDTO nodeDTO, MultiValueMap<LinkID, InsertLocation> locations) {
            super(nodeDTO, locations);
        }

        @Override
        public String toString() {
            return "InsertChange(" + nodeDTO + " " + locations + ")";
        }
    }

    public interface ReferencedInsertChange {
        int changeTaskReference();
        TaskNodeDTO nodeDTO();
    }

    /*
     * Persists a new TaskNode that does not exist in the database.
     * References another change that is expected to return a Task.
     */

    @Getter
    public static class ReferencedInsertAtChange extends InsertAtChange implements ReferencedInsertChange {
        private final int changeTaskReference;

        public ReferencedInsertAtChange(TaskNodeDTO nodeDTO, MultiValueMap<LinkID, InsertLocation> locations, int changeTaskReference) {
            super(nodeDTO, locations);
            this.changeTaskReference = changeTaskReference;
        }

        public ReferencedInsertAtChange(TaskNodeDTO nodeDTO, @NonNull LinkID reference, InsertLocation location, int changeTaskReference) {
            super(nodeDTO, reference, location);
            this.changeTaskReference = changeTaskReference;
        }
    }

    /*
     * Adds an existing Task to a new location in the Task tree based on a Task ID reference.
     */
    @Getter
    public static class InsertIntoChange extends InsertChange<TaskID> {
        public InsertIntoChange(TaskNodeDTO nodeDTO, TaskID reference, InsertLocation location) {
            super(nodeDTO, reference, location);
        }

        public InsertIntoChange(TaskNodeDTO nodeDTO, MultiValueMap<TaskID, InsertLocation> locations) {
            super(nodeDTO, locations);
        }

        @Override
        public String toString() {
            return "InsertIntoChange(" + nodeDTO + " " + locations + ")";
        }
    }

    @Getter
    public static class ReferencedInsertIntoChange extends InsertIntoChange implements ReferencedInsertChange {
        private final int changeTaskReference;

        public ReferencedInsertIntoChange(TaskNodeDTO nodeDTO, MultiValueMap<TaskID, InsertLocation> locations, int changeTaskReference) {
            super(nodeDTO, locations);
            this.changeTaskReference = changeTaskReference;
        }

        public ReferencedInsertIntoChange(TaskNodeDTO nodeDTO, TaskID reference, InsertLocation location, int changeTaskReference) {
            super(nodeDTO, reference, location);
            this.changeTaskReference = changeTaskReference;
        }
    }

    /*
     * Moves an existing TaskLink to additional locations in the Task tree, and removes it from its previous location.
     */
    @Getter
    public static class MoveChange extends Change {
        private final LinkID originalId;
        private final MultiValueMap<LinkID, InsertLocation> locations;

        public MoveChange(LinkID originalId, LinkID reference, InsertLocation location) {
            this.originalId = originalId;
            this.locations = new LinkedMultiValueMap<>();
            locations.add(reference, location);
        }

        @Override
        public String toString() {
            return "MoveChange(" + originalId + " " + locations + ")";
        }
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

    /*
     * Copies an existing TaskNode and its children (after filter) to additional locations in the Task tree.
     * Can either be shallow (descendants refer to same tasks as previous) or deep (all tasks are new and separate instances).
     */
    @Getter
    public static class CopyChange extends Change {
        protected final CopyType copyType;
        protected final LinkID originalId;
        protected final MultiValueMap<LinkID, InsertLocation> locations;
        protected final TaskTreeFilter taskFilter;
        protected final String suffix;

        public enum CopyType {
            SHALLOW,
            DEEP
        }

        public CopyChange(CopyType copyType, LinkID originalId, LinkID reference, InsertLocation location, TaskTreeFilter taskFilter, String suffix) {
            this.copyType = copyType;
            this.originalId = originalId;
            this.locations = new LinkedMultiValueMap<>();
            this.locations.add(reference, location);
            this.taskFilter = taskFilter;
            this.suffix = suffix;
        }
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
