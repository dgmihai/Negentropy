package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.model.TaskNode;
import com.vaadin.flow.function.SerializableConsumer;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public interface TaskNodeView {
    HashMap<Class<?>, List<SerializableConsumer<?>>> listeners = new HashMap<>();

    Optional<TaskNode> rootNode();
}