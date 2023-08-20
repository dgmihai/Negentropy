package com.trajan.negentropy.model.entity;

import com.trajan.negentropy.model.data.HasTaskData.TaskData;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.server.backend.sync.SyncManagerListener;
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
@EntityListeners(SyncManagerListener.class)
@Table(name = "tasks")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
public class TaskEntity extends AbstractEntity implements TaskData<TaskEntity, TagEntity> {

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
    private Boolean required = false;

    @ToString.Include
    private Boolean project = false;

    @OneToMany(
            mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<NetDuration> netDurations = new ArrayList<>();

    @OneToMany(
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
