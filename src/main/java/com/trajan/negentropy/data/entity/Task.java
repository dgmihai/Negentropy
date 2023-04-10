package com.trajan.negentropy.data.entity;

import com.trajan.negentropy.view.util.DurationConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    @NotBlank
    private String name;
    private String description = "";
    private Duration duration = Duration.ZERO;
    private int priority = 0;

    @ManyToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @ManyToMany(
            mappedBy = "children",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    private List<Task> parents = new ArrayList<>();

    @ManyToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "subtasks",
            joinColumns=@JoinColumn(name = "child_id"),
            inverseJoinColumns=@JoinColumn(name = "parent_id"))
    @OrderColumn
    private List<Task> children = new ArrayList<>();

    @Transient
    private Task addToChildrenOf;

    @Transient
    private Task removeFromChildrenOf;

    public void newParent(Task parent) {
        if (!parents.isEmpty()) {
            removeFromChildrenOf = parents.remove(0);
        }
        addToChildrenOf = parent;
    }

    public void log() {
        logger.debug("Task Logged::");
        logger.debug("id         ={}", this.getId());
        logger.debug("name       ={}", this.getName());
        logger.debug("description={}", this.getDescription());
        logger.debug("duration   ={}", DurationConverter.toPresentation(this.getDuration()));
        logger.debug("priority   ={}", this.getPriority());
        logger.debug("tags       =");
        for (Tag tag : this.getTags()) {
            logger.debug("  tag: id={}, name={}", tag.getId(), tag.getName());
        }
        logger.debug("parents    =");
        for (Task parent : this.getParents()) {
            logger.debug("  parent: id={}, name={}", parent.getId(), parent.getName());
        }
        logger.debug("children   =");
        for (Task child : this.getChildren()) {
            logger.debug("  child: id={}, name={}", child.getId(), child.getName());
        }
    }
}
