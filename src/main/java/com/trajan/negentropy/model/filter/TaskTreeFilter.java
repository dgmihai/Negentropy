package com.trajan.negentropy.model.filter;

import com.trajan.negentropy.model.id.TagID;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class TaskTreeFilter implements Serializable {
    @ToString.Exclude
    private static final long serialVersionUID = 1L;

    protected String name;
    protected Set<TagID> includedTagIds = new HashSet<>();
    protected Set<TagID> excludedTagIds = new HashSet<>();
    protected Set<String> options = new HashSet<>();
    protected Boolean ignoreScheduling;

    public static final String ONLY_REQUIRED = "Only Required";
    public static final String ONLY_PROJECTS = "Only Projects";
    public static final String ALWAYS_INCLUDE_PARENTS = "Include Parents";
    public static final String INNER_JOIN_INCLUDED_TAGS = "Inner Join Included Tags";

    public TaskTreeFilter(String... options) {
        this.options.addAll(Set.of(options));
    }

    public static Set<String> OPTION_TYPES() {
        return Set.of(
                ONLY_REQUIRED,
                ONLY_PROJECTS,
                ALWAYS_INCLUDE_PARENTS,
                INNER_JOIN_INCLUDED_TAGS);
    }

    @Override
    public String toString() {
        String includedTags = includedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String excludedTags = excludedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String opts = String.join(", ", options);
        return "TaskTreeFilter{" +
                "name='" + name + '\'' +
                ", includedTagIds=[" + includedTags + "]" +
                ", excludedTagIds=[" + excludedTags + "]" +
                ", options=[" + opts + "]" +
                '}';
    }

    public boolean isEmpty() {
        return this.equals(new TaskTreeFilter());
    }
}