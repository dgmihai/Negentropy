package com.trajan.negentropy.server.entity;

import com.trajan.negentropy.client.util.DurationConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TaskInfo extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfo.class);
    @Column(name = "title", nullable = false, unique = true)
    @NotEmpty(message = "Title is required")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description = "";
    private Duration duration = Duration.ZERO;
    private Integer priority = 0;

    @ToString.Exclude
    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "taskInfo",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @NotEmpty
    private List<TaskRelationship> relationships = new ArrayList<>();

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "taskInfo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    public void addRelationship(TaskRelationship relationship) {
        if(!this.relationships.contains(relationship)) {
            relationships.add(relationship.getOrderIndex(), relationship);
            relationship.setTaskInfo(this);
        }
    }

    public void removeRelationship(TaskRelationship relationship) {
        relationships.remove(relationship);
        relationship.setTaskInfo(null);
    }

    public void log() {
        log("Task Logged::");
    }

    public void log(String prefix) {
        logger.debug(prefix);
        logger.debug("id=          {}", this.getId());
        logger.debug("title=       {}", this.getTitle());
    }

    public void trace(String prefix) {
        log(prefix);
        if (this.getDescription() != null && !this.getDescription().isEmpty()) {
            logger.debug("description= {}", this.getDescription());
        }
        if (!this.getDuration().isZero()) {
            logger.debug("duration=    {}", DurationConverter.toPresentation(this.getDuration()));
        }
        if (this.getPriority() != null) {
            logger.debug("priority=    {}", this.getPriority());
        }
        if (this.getTags() != null && !this.getTags().isEmpty()) {
            logger.debug("tags=");
            for (Tag tag : this.getTags()) {
                logger.debug("  tag: id={}, name={}", tag.getId(), tag.getName());
            }
        }
    }



}
