package com.trajan.negentropy.model.entity;

import com.trajan.negentropy.model.data.HasTaskData.TaskData;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.interfaces.HasDuration;
import com.trajan.negentropy.model.interfaces.HasTaskLinkOrTaskEntity;
import com.trajan.negentropy.server.backend.sync.SyncManagerListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Duration;
import java.util.*;

@Entity
@EntityListeners(SyncManagerListener.class)
@Table(name = "tasks", indexes = {
        @Index(columnList = "name", name = "idx_task_name"),
        @Index(columnList = "starred", name = "idx_task_starred"),
        @Index(columnList = "pinned", name = "idx_task_pinned"),
        @Index(columnList = "project", name = "idx_task_project")
})
@NoArgsConstructor
@Getter
@Setter
@Slf4j
public class TaskEntity extends AbstractEntity implements TaskData<TaskEntity>, HasDuration, HasTaskLinkOrTaskEntity {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "task_seq", sequenceName = "task_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_seq")
    private Long id;
    
    @Column(nullable = false, unique = true)
    @NotEmpty(message = "Name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description = "";

    private Duration duration = Duration.ZERO;
    private Boolean required = false;
    private Boolean project = false;
    private Boolean difficult = false;
    private Boolean starred = false;
    private Boolean pinned = false;
    private Boolean cleanup = false;

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
            fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE)
    @JoinTable(
            name = "task_tags",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Fetch(FetchMode.SUBSELECT)
    private Set<TagEntity> tags = new HashSet<>();

    @Override
    public String toString() {
        return "TaskEntity(" + id + ")[name=" + name + ", description=" + description + ", duration=" + duration
                + ", required=" + required + ", project=" + project + ", difficult=" + difficult + ", starred="
                + starred + ", pinned=" + pinned + ", cleanup=" + cleanup + "]";
    }

    @Override
    public Optional<TaskLink> link() {
        return Optional.empty();
    }

    @Override
    public TaskEntity task() {
        return this;
    }
}
