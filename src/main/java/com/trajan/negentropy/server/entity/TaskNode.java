package com.trajan.negentropy.server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "task_nodes")
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TaskNode extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskNode.class);
    @ManyToOne
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @ManyToOne
    @JoinColumn(name = "data_task_id")
    @NotNull
    private Task referenceTask;

    @ManyToOne(fetch = FetchType.EAGER,
            cascade = {
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    @JoinColumn(name = "prev_id")
    private TaskNode prev;

    @ManyToOne(fetch = FetchType.EAGER,
            cascade = {
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    @JoinColumn(name = "next_id")
    private TaskNode next;

    @Builder.Default
    private Integer priority = 0;

    public String toString() {
        return "Node(" + super.toString() + "," + referenceTask + ")";
    }

    public void log() {
        log("== TaskNode Logged::");
    }

    public void log(String prefix) {
        logger.info(prefix);
        logger.info("ID:     " + (getId() == null ? "null" : getId()));
//        logger.info("Parent: " + (getParentNode() == null ? "null" : getParentNode().getId()));
        logger.info("Child:  " + (getReferenceTask() == null ? "null" : getReferenceTask().getId()));
        logger.info("Prev:   " + (getPrev() == null ? "null" : getPrev().getId()));
        logger.info("Next:   " + (getNext() == null ? "null" : getNext().getId()));
    }
}
