package com.trajan.negentropy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.scheduling.support.CronExpression;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@ToString
public class GenericTaskNodeInfo<T extends GenericTaskNodeInfo<T>> {
    protected Integer importance;

    protected Boolean completed;

    protected Boolean recurring;
    protected CronExpression cron;

    @SuppressWarnings("unchecked")
    public T importance(Integer importance) {
        this.importance = importance;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T completed(Boolean completed) {
        this.completed = completed;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T recurring(Boolean recurring) {
        this.recurring = recurring;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T cron(CronExpression cron) {
        this.cron = cron;
        return (T) this;
    }
}