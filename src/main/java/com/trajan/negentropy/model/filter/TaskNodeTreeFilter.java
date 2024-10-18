package com.trajan.negentropy.model.filter;

import com.trajan.negentropy.model.id.TagID;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TaskNodeTreeFilter extends TaskTreeFilter implements Serializable {
    private static final long serialVersionUID = 2L;

    protected LocalDateTime completedBefore;
    protected Boolean completed;
    protected Boolean recurring;
    protected LocalDateTime availableAtTime;
    protected Integer importanceThreshold;

    public TaskNodeTreeFilter(String... options) {
        super(options);
    }

//    @Override
//    public String toString() {
//        String includedTags = includedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
//        String excludedTags = excludedTagIds.stream().map(TagID::toString).collect(Collectors.joining(", "));
//        String opts = String.join(", ", options);
//        return "TaskNodeTreeFilter{" +
//                "name='" + name + '\'' +
//                ", includedTagIds=[" + includedTags + "]" +
//                ", excludedTagIds=[" + excludedTags + "]" +
//                ", availableAtTime=" + availableAtTime +
//                ", importanceThreshold=" + importanceThreshold +
//                ", durationLimit=" + durationLimit +
//                ", options=[" + opts + "]" +
//                '}';
//    }

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

    @Override
    public TaskNodeTreeFilter hasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
        return this;
    }

    public TaskNodeTreeFilter availableAtTime(LocalDateTime availableAtTime) {
        availableAtTime = availableAtTime == null
                ? null
                : availableAtTime.truncatedTo(ChronoUnit.HOURS);
        this.availableAtTime = availableAtTime;
        return this;
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    public static class NestableTaskNodeTreeFilter extends TaskNodeTreeFilter {
        private boolean nested = true;
    }

}
