package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tasks")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
public class TaskEntity extends AbstractEntity {

    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Name is required")
    @ToString.Include
    private String name;

    @Column(columnDefinition = "TEXT")
    @ToString.Include
    private String description = "";

    @ToString.Include
    private Duration duration = Duration.ZERO;

    @ToString.Include
    private boolean block = false;

    @ToString.Include
    private boolean isProject = false;

    // TODO: Unidirectional
    @OneToOne(
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    private TaskEntity projectOwner = null;

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<TotalDurationEstimate> timeEstimates = new ArrayList<>();

    @OneToMany(
            fetch = FetchType.EAGER,
            mappedBy = "parent",
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("position")
    private List<TaskLink> childLinks = new ArrayList<>();

    @OneToMany(
            mappedBy = "child",
            cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<TaskLink> parentLinks = new ArrayList<>();

    @ManyToMany(
            fetch = FetchType.EAGER,
            cascade = CascadeType.REMOVE)
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Fetch(FetchMode.SUBSELECT)
    private Set<TagEntity> tags = new HashSet<>();
}
