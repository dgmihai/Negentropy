package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Entity
@Table(name = "task_info")
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Accessors(fluent = true)
@Getter
@Setter
public class TaskEntity extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskEntity.class);

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Name is required")
    private String name;

    @Builder.Default
    private String description = "";

    @Builder.Default
    private Duration duration = Duration.ZERO;

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("position")
    @Builder.Default
    private List<TaskLinkEntity> childLinks = new ArrayList<>();

    public Stream<TaskEntity> children() {
        return childLinks.stream().map(TaskLinkEntity::child);
    }

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "child",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<TaskLinkEntity> parentLinks = new ArrayList<>();

    public Stream<TaskEntity> parents() {
        return parentLinks.stream().map(TaskLinkEntity::parent);
    }

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "taskInfo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<TagEntity> tags = new LinkedHashSet<>();

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
