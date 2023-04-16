package com.trajan.negentropy.server.entity;

import jakarta.persistence.*;
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
    @JoinColumn(name = "parent_id")
    private Task parent;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task data;

    @ManyToOne(cascade = {
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    @JoinColumn(name = "prev_id")
    private TaskNode prev;

    @ManyToOne(cascade = {
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
            CascadeType.DETACH})
    @JoinColumn(name = "next_id")
    private TaskNode next;

    public String toString() {
        return "Node(" + super.toString() + ")";
    }

    public void log() {
        log("== TaskNode Logged::");
    }

    public void log(String prefix) {
        logger.info(prefix);
        logger.info("ID:     " + (getId() == null ? "null" : getId()));
        logger.info("Parent: " + (getParent() == null ? "null" : getParent().getId()));
        logger.info("Child:  " + (getData() == null ? "null" : getData().getId()));
        logger.info("Prev:   " + (getPrev() == null ? "null" : getPrev().getId()));
        logger.info("Next:   " + (getNext() == null ? "null" : getNext().getId()));
    }
}
