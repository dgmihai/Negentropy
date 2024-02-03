package com.trajan.negentropy.model.filter;

import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class RoutineLimitFilter extends TaskNodeTreeFilter {
    protected Duration durationLimit;
    protected Integer stepCountLimit;
    protected LocalDateTime etaLimit;

    public static RoutineLimitFilter parse(TaskNodeTreeFilter filter) {
        if (filter instanceof RoutineLimitFilter) {
            return (RoutineLimitFilter) filter;
        } else {
            return filter != null
                    ? (RoutineLimitFilter) new RoutineLimitFilter()
                    .name(filter.name())
                    .completed(filter.completed())
                    .recurring(filter.recurring())
                    .availableAtTime(filter.availableAtTime())
                    .importanceThreshold(filter.importanceThreshold())
                    .includedTagIds(filter.includedTagIds())
                    .excludedTagIds(filter.excludedTagIds())
                    .ignoreScheduling(filter.ignoreScheduling())
                    .options(filter.options())
                    : new RoutineLimitFilter();
        }
    }

    public boolean isLimiting() {
        return durationLimit != null || stepCountLimit != null || etaLimit != null;
    }
}
