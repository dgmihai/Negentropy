package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "task_links")
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Accessors(fluent = true)
@Getter
@Setter
public class TaskLinkEntity extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskLinkEntity.class);

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TaskEntity parent;

    @ManyToOne
    @JoinColumn(name = "value_id")
    @NotNull
    private TaskEntity child;

    @Builder.Default
    private Integer position = 0;

    @Builder.Default
    private Integer priority = 0;

    public String toString() {
        return "LinkEntity(" + super.toString() + ", Parent" + parent + ", Child" + child + ")";
    }

    public void log() {
        log("== TaskLink Logged::");
    }

    public void log(String prefix) {
        logger.info(prefix);
        logger.info("ID:     " + (id() == null ? "null" : id()));
//        logger.info("Parent: " + (getParentNode() == null ? "null" : getParentNode().getId()));
        logger.info("Child:  " + (child() == null ? "null" : child().id()));
    }
}
