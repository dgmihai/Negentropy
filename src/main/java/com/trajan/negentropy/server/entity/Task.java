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
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Task extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Title is required")
    private String title;

    @Builder.Default
    private String description = "";

    private Duration duration = Duration.ZERO;

    @ManyToMany(
            cascade = CascadeType.REMOVE,
            fetch = FetchType.EAGER)
    @JoinTable(
            name = "taggings",
            joinColumns = @JoinColumn(name = "taskInfo_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags = new ArrayList<>();

    public String toString() {
        return "Task(" + super.toString() + ", title: " + title + ")";
    }

    public void log() {
        log("Task Logged::");
    }

    public void log(String prefix) {
        logger.debug(prefix);
        logger.debug("id=          {}", this.getId());
        logger.debug("title=       {}", this.getTitle());
        logger.debug("duration=    {}", this.getDuration());
    }
}
