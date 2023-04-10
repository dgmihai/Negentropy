package com.trajan.negentropy.data.entity;

import com.trajan.negentropy.view.util.DurationConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Task extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    @NotBlank
    private String name;
    private String description = "";
    private Duration duration = Duration.ZERO;
    private Integer priority = 0;

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new LinkedHashSet<>();

    @ToString.Exclude
    @ManyToMany(
            mappedBy = "children")
    private List<Task> parents = new ArrayList<>();

    @ManyToMany(
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "subtasks",
            joinColumns=@JoinColumn(name = "child_id"),
            inverseJoinColumns=@JoinColumn(name = "parent_id"))
    @OrderColumn
    private List<Task> children = new ArrayList<>();

    // Implementation that _probably_ shouldn't be in the Entity

//    @Transient
//    private Task addToChildrenOf;
//
//    @Transient
//    private Task removeFromChildrenOf;
//
//    public void newParent(Task parent) {
//        if (!parents.isEmpty()) {
//            removeFromChildrenOf = parents.remove(0);
//        }
//        addToChildrenOf = parent;
//    }

    public void log() {
        log("Task Logged::");
    }

    
    public void trace(String title) {
        log(title);
        if (this.getPk() != null) {
            logger.debug("pk=          {}", this.getPk());
        }
        if (this.getDescription() != null && !this.getDescription().isEmpty()) {
            logger.debug("description= {}", this.getDescription());
        }
        if (!this.getDuration().isZero()) {
            logger.debug("duration=    {}", DurationConverter.toPresentation(this.getDuration()));
        }
        if (this.getPriority() != 0) {
            logger.debug("priority=    {}", this.getPriority());
        }        
        if (this.getTags() != null && !this.getTags().isEmpty()) {
            logger.debug("tags=");
            for (Tag tag : this.getTags()) {
                logger.debug("  tag: id={}, name={}", tag.getId(), tag.getName());
            }
        }
//        if (this.getParents() != null && !this.getParents().isEmpty()) {
//            logger.debug("parents=");
//            for (Task parent : this.getParents()) {
//                logger.debug("  id={}, name={}", parent.getId(), parent.getName());
//            }
//        }
        if (this.getChildren() != null && !this.getChildren().isEmpty()) {
            logger.debug("children=");
            for (Task child : this.getChildren()) {
                logger.debug("  id={}, name={}", child.getId(), child.getName());
            }
        }
    }
    
    public void log(String title) {
        logger.debug(title);

        logger.debug("id=          {}", this.getId());
        if (this.getName() != null && !this.getName().isEmpty()) {
            logger.debug("name=        {}", this.getName());
        }
        if (this.getInstanceParent() != null) {
            logger.debug("parent=      id={}, name={}", instanceParent.getId(), instanceParent.getName());
        }
    }

    @Getter(AccessLevel.PRIVATE)
    @Setter(AccessLevel.PRIVATE)
    @Transient
    private Duration calculatedDuration = Duration.ZERO;

    public Duration getDuration() {
        if (children.isEmpty()) {
            return duration;
        } else {
            if (calculatedDuration.isZero()) {
                for (Task child : children) {
                    calculatedDuration = calculatedDuration.plus(child.getDuration());
                }
            }
            return calculatedDuration.isZero() ? duration : calculatedDuration;
        }
    }

    @Transient
    @ToString.Exclude
    private Task instanceParent;

//    public Task shallowCopy(Task original) {
//        logger.debug("Copy initial Id: {}", original.getId());
//        Task copy = original.toBuilder().build();
//        logger.debug("Copy new Id: {}", this.getId());
//        return copy;
//    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Task other)) {
            return false;
        }
        return Objects.equals(getId(), other.getId()) &&
                Objects.equals(instanceParent, other.instanceParent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), instanceParent != null ? instanceParent.getId() : null);
    }
}
