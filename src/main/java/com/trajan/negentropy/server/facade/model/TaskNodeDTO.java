package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.scheduling.support.CronExpression;

@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class TaskNodeDTO extends TaskNodeInfo {
    private TaskID parentId;
    private TaskID childId;

    public TaskNodeDTO(TaskID parentId, TaskID childId, Integer position, Integer importance,
                       Boolean completed, Boolean recurring, CronExpression cron) {
        this.parentId = parentId;
        this.childId = childId;
        this.position = position;
        this.importance = importance;
        this.completed = completed;
        this.recurring = recurring;
        this.cron = cron;
    }


    public TaskNodeDTO(TaskNode node) {
        this.parentId = node.parentId();
        this.childId = node.child().id();
        this.position = node.position();
        this.importance = node.importance();
        this.completed = node.completed();
        this.recurring = node.recurring();
        this.cron = node.cron();
    }

    public TaskNodeDTO(TaskID parentId, TaskID childId, TaskNodeInfo info) {
        this.parentId = parentId;
        this.childId = childId;
        if (info != null) {
            this.position = info.position();
            this.importance = info.importance();
            this.completed = info.completed();
            this.recurring = info.recurring();
            this.cron = info.cron();
        }
    }
}
