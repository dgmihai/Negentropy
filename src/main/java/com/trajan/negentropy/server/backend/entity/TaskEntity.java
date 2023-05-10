package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TaskEntity extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntity.class);

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    private Duration duration = Duration.ZERO;

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<TimeEstimate> timeEstimates = new ArrayList<>();

    @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL)
    @OrderBy("position")
    private List<TaskLink> childLinks = new ArrayList<>();

    @OneToMany(
            mappedBy = "child",
            cascade = CascadeType.ALL)
    private List<TaskLink> parentLinks = new ArrayList<>();

    @ManyToMany(
            cascade = CascadeType.REMOVE)
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    private Boolean oneTime;

    @Override
    public String toString() {
        return "TaskEntity[" + super.toString() + ", name=" + name + "]";
    }
}
