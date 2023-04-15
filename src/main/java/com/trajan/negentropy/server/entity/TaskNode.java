package com.trajan.negentropy.server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "task_nodes")
@RequiredArgsConstructor
@Getter
@Setter
public class TaskNode extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskNode.class);

    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = CascadeType.MERGE)
    @JoinColumn(name = "parent_id")
    private TaskInfo parent;

    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = CascadeType.MERGE)
    @JoinColumn(name = "child_id")
    private TaskInfo child;

    @OneToOne(fetch = FetchType.EAGER,
            cascade = {CascadeType.MERGE,
                       CascadeType.PERSIST})
    @JoinColumn(
            name = "prev_node_id",
            referencedColumnName = "id")
    private TaskNode prev;

    @OneToOne(fetch = FetchType.EAGER,
            cascade = {CascadeType.MERGE,
                       CascadeType.PERSIST})
    @JoinColumn(
            name = "next_node_id",
            referencedColumnName = "id")
    // This is treated as the source of truth in case of conflicts between this & prev
    private TaskNode next;

    @Builder
    public TaskNode(TaskInfo parent, TaskInfo child, TaskNode next) {
        this.parent=parent;
        this.child=child;
        this.next=next;
    }

    public void log() {
        log("TaskNode Logged::");
    }

    public void log(String prefix) {
        logger.debug(prefix);
        logger.debug("id=          {}", this.getId());
        logger.debug("parent_id=   {}", this.getParent().getId());
        logger.debug("parent_id=   {}", this.getParent().getId());
        logger.debug("parent_title={}", this.getParent().getTitle());
        logger.debug("child_id=    {}", this.getChild().getId());
        logger.debug("child_title= {}", this.getChild().getTitle());
    }
}
