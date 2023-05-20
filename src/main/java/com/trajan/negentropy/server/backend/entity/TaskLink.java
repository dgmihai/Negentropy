package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "task_links")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TaskLink extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskLink.class);

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TaskEntity parent;

    @ManyToOne
    @JoinColumn(name = "child_id")
    @NotNull
    private TaskEntity child;

    private Integer position = 0;

    private Integer importance = 0;

    private Boolean recurring = false;

    public String toString() {
        return "LinkEntity[" + super.toString() + ", parent=" + parent + ", child=" + child + "]";
    }
}
