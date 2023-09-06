package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.TaskNode;

import java.util.Optional;

public interface MayHaveTaskNodeData extends HasTaskData {
    Optional<TaskNode> nodeOptional();
}
