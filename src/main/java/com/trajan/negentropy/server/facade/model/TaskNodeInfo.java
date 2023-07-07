package com.trajan.negentropy.server.facade.model;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.scheduling.support.CronExpression;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class TaskNodeInfo {
    protected Integer position;
    protected Integer importance;

    protected Boolean completed;

    protected Boolean recurring;
    protected CronExpression cron;
}