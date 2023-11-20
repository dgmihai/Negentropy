package com.trajan.negentropy.model.filter;

import lombok.ToString;

@ToString(callSuper = true)
public class NonSpecificTaskNodeTreeFilter extends TaskNodeTreeFilter {
    public NonSpecificTaskNodeTreeFilter() {
        super();
        this.name = null;
        this.completed = false;
        this.ignoreScheduling = false;
    }

    public static NonSpecificTaskNodeTreeFilter parse(TaskNodeTreeFilter filter) {
        if (filter instanceof NonSpecificTaskNodeTreeFilter) {
            return (NonSpecificTaskNodeTreeFilter) filter;
        } else {
            return filter != null
                    ? (NonSpecificTaskNodeTreeFilter) new NonSpecificTaskNodeTreeFilter()
                    .name(null) // We explicitly exclude name from the filter
                    .completed(false)
                    .recurring(filter.recurring())
                    .availableAtTime(filter.availableAtTime())
                    .importanceThreshold(filter.importanceThreshold())
                    .includedTagIds(filter.includedTagIds())
                    .excludedTagIds(filter.excludedTagIds())
                    .ignoreScheduling(false)
                    .options(filter.options())
                    : new NonSpecificTaskNodeTreeFilter();
        }
    }

    public String name() {
        return "";
    }

    public NonSpecificTaskNodeTreeFilter name(String name) {
        return this;
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
}
