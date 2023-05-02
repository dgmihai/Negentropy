package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Entity
@Table(name = "task_info")
@RequiredArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TaskEntity extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntity.class);

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Name is required")
    private String name;

    private String description = "";

    private Duration duration = Duration.ZERO;

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("position")
    private List<TaskLink> childLinks = new ArrayList<>();

    public Stream<TaskEntity> children() {
        return childLinks.stream().map(TaskLink::child);
    }

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "child",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<TaskLink> parentLinks = new ArrayList<>();

    public Stream<TaskEntity> parents() {
        return parentLinks.stream().map(TaskLink::parent);
    }

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<TagEntity> tags = new HashSet<>();

    public TaskEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TaskEntity(" + super.toString() + ", name: " + name + ")";
    }

    public void log() {
        log("Task Logged::");
    }

    public void log(String prefix) {
        logger.debug(prefix);
        logger.debug("id=          {}", this.id());
        logger.debug("title=       {}", this.name());
        logger.debug("duration=    {}", this.duration());
    }
}
