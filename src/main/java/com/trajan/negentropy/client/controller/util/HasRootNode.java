package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.model.TaskNode;

import java.util.Optional;

public interface HasRootNode {
    Optional<TaskNode> rootNode();
}