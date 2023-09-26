package com.trajan.negentropy.model.filter;

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
}
