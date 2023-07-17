package com.trajan.negentropy.server.facade.model;

import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;

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

    protected Duration projectDuration;
}