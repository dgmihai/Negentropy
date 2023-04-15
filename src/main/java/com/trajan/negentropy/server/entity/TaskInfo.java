package com.trajan.negentropy.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

@Entity
@Table(name = "task_info")
@Builder(toBuilder = true, builderMethodName = "hiddenBuilder")
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TaskInfo extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskInfo.class);

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Title is required")
    private String title;
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String description = "";
    @Builder.Default
    private Duration duration = Duration.ZERO;
    @Builder.Default
    private Integer priority = 0;

    @ToString.Exclude
    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "child",
            orphanRemoval = true,
            cascade = CascadeType.MERGE)
    private List<TaskNode> nodes = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parent",
            orphanRemoval = true,
            cascade = CascadeType.MERGE)
    @OrderBy("prev") // prev = null is the head of the list
    @Transient
    private List<TaskNode> children = new LinkedList<>();

    /* We are making strong assumptions that the 'children' list:
       - Contains a list of elements that form a complete and whole doubly linked list
       - That the first item in children will have 'prev' set to null, as the starting node
     */
    @PostLoad
    private void orderChildren() {
        if (children.size() > 1) {
            LinkedList<TaskNode> orderedChildren = new LinkedList<>();
            TaskNode current = children.get(0);
            for (int i = 0; i < children.size(); i++) {
                TaskNode next = current.getNext();
                // We aren't we checking for null? We are assuming data is coherent
                orderedChildren.add(current);
                current = current.getNext();
            }
            children = orderedChildren;
        }
    }

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "taskInfo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    public void log() {
        log("TaskInfo Logged::");
    }

    public void log(String prefix) {
        logger.debug(prefix);
        logger.debug("id=          {}", this.getId());
        logger.debug("title=       {}", this.getTitle());
        logger.debug("duration=    {}", this.getDuration());
        logger.debug("priority=    {}", this.getPriority());
        logger.debug("# children=  {}", this.getChildren().size());
        logger.debug("# nodes size={}", this.getNodes().size());
    }
}
