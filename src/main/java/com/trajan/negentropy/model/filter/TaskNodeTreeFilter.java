package com.trajan.negentropy.model.filter;

import com.trajan.negentropy.model.id.TagID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
public class TaskNodeTreeFilter extends TaskTreeFilter {
    private Boolean completed;
    private LocalDateTime availableAtTime;
    private Integer importanceThreshold;
    private Duration durationLimit;

    public TaskNodeTreeFilter(String... options) {
        super(options);
    }

    @Override
    public String toString() {
        String includedTags = includedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String excludedTags = excludedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
        String opts = String.join(", ", options);
        return "TaskNodeTreeFilter{" +
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
        return this.equals(new TaskNodeTreeFilter());
    }

    @Override
    public TaskNodeTreeFilter name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public TaskNodeTreeFilter includedTagIds(Set<TagID> includedTagIds) {
        this.includedTagIds = includedTagIds;
        return this;
    }

    @Override
    public TaskNodeTreeFilter excludedTagIds(Set<TagID> excludedTagIds) {
        this.excludedTagIds = excludedTagIds;
        return this;
    }

    @Override
    public TaskNodeTreeFilter options(Set<String> options) {
        this.options = options;
        return this;
    }
}
