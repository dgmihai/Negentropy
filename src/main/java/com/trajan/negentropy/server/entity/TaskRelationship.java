package com.trajan.negentropy.server.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_relationships")
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TaskRelationship extends AbstractEntity {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    @Nullable
    private TaskRelationship parentRelationship;

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parentRelationship",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("orderIndex ASC")
    private List<TaskRelationship> childRelationships = new ArrayList<>();

    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST,
                       CascadeType.MERGE})
    @JoinColumn(name = "task_id")
    private TaskInfo taskInfo;

    private int orderIndex = 0;

    public TaskRelationship(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }

    public void addChildRelationship(TaskRelationship child) {
        childRelationships.add(child);
        child.setParentRelationship(this);
    }

    public void removeChildRelationship(TaskRelationship child) {
        childRelationships.remove(child);
        child.setParentRelationship(null);
    }
}
