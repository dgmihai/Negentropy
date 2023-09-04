package com.trajan.negentropy.model.filter;

import com.trajan.negentropy.model.id.TagID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
public class TaskTreeFilter {
    private String name;
    private Set<TagID> includedTagIds = new HashSet<>();
    private Set<TagID> excludedTagIds = new HashSet<>();
    private LocalDateTime availableAtTime;
    private Integer importanceThreshold;
    private Duration durationLimit;
    private Set<String> options = new HashSet<>();

    public static final String ONLY_REQUIRED = "Only Required";
    public static final String ONLY_PROJECTS = "Only Projects";
    public static final String HIDE_COMPLETED = "Hide Completed Tasks";
    public static final String ALWAYS_INCLUDE_PARENTS = "Include Parents";
    public static final String IGNORE_SCHEDULING = "Ignore Scheduling";
    public static final String INNER_JOIN_INCLUDED_TAGS = "Inner Join Included Tags";

    // Hidden
    public static final String WITH_PROJECT_DURATION_LIMITS = "With Project Duration Limits";

    public TaskTreeFilter(String... options) {
        this.options.addAll(Set.of(options));
    }

    public static Set<String> OPTION_TYPES() {
        return Set.of(
                ONLY_REQUIRED,
                ONLY_PROJECTS,
                HIDE_COMPLETED,
                ALWAYS_INCLUDE_PARENTS,
                IGNORE_SCHEDULING,
                INNER_JOIN_INCLUDED_TAGS);
    }

    @Override
    public String toString() {
        String includedTags = includedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String excludedTags = excludedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String opts = String.join(", ", options);
        return "TaskFilter{" +
                "name='" + name + '\'' +
                ", includedTagIds=[" + includedTags + "]" +
                ", excludedTagIds=[" + excludedTags + "]" +
                ", availableAtTime=" + availableAtTime +
                ", importanceThreshold=" + importanceThreshold +
                ", durationLimit=" + durationLimit +
                ", options=[" + opts + "]" +
                '}';
    }

    public boolean isEmpty() {
        return this.equals(new TaskTreeFilter());
    }
}