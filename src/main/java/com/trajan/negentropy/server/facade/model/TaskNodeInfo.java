package com.trajan.negentropy.server.facade.model;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@ToString
public class TaskNodeInfo {
    protected Integer position;
    protected Integer importance;
    protected Boolean recurring;
    protected Boolean completed;
}
