package com.trajan.negentropy.model.filter;

import lombok.ToString;

@ToString(callSuper = true)
public class NonSpecificTaskNodeTreeFilter extends TaskNodeTreeFilter {
    public NonSpecificTaskNodeTreeFilter() {
        super();
        this.name = null;
    }

    public static NonSpecificTaskNodeTreeFilter from(TaskNodeTreeFilter filter) {
        return filter != null
                ? (NonSpecificTaskNodeTreeFilter) new NonSpecificTaskNodeTreeFilter()
                        .name("") // We explicitly exclude name from the filter
                        .completed(false)
                        .recurring(filter.recurring())
                        .availableAtTime(filter.availableAtTime())
                        .importanceThreshold(filter.importanceThreshold())
                        .durationLimit(null)// We also explicitly exclude durationLimit
                        .includedTagIds(filter.includedTagIds())
                        .excludedTagIds(filter.excludedTagIds())
                        .ignoreScheduling(filter.ignoreScheduling())
                        .options(filter.options())
                : new NonSpecificTaskNodeTreeFilter();
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
