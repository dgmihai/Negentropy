package com.trajan.negentropy.server.facade.model.filter;

import com.trajan.negentropy.server.facade.model.id.TagID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
public class TaskFilter {
    private String name;
    private Set<TagID> includedTagIds;
    private Set<TagID> excludedTagIds;
    private LocalDateTime availableAtTime;
    private Integer importanceThreshold;
    private Set<String> options = new HashSet<>();

    public static final String ONLY_BLOCKS = "Only Blocks";
    public static final String ONLY_PROJECTS = "Only Projects";
    public static final String DONT_HIDE_COMPLETED = "Show Completed Tasks";
    public static final String ALWAYS_INCLUDE_PARENTS = "Include Parents";
    public static final String IGNORE_SCHEDULING = "Ignore Scheduling";
    public static final String INNER_JOIN_INCLUDED_TAGS = "Inner Join Included Tags";

    public TaskFilter(String... options) {
        this.options.addAll(Set.of(options));
    }

    public static Set<String> OPTION_TYPES() {
        return Set.of(
                ONLY_BLOCKS,
                ONLY_PROJECTS,
                DONT_HIDE_COMPLETED,
                ALWAYS_INCLUDE_PARENTS,
                IGNORE_SCHEDULING,
                INNER_JOIN_INCLUDED_TAGS);
    }
}