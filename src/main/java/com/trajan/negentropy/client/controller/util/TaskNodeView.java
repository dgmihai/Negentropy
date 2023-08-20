package com.trajan.negentropy.client.controller.util;

import com.trajan.negentropy.model.id.LinkID;

import java.util.Optional;

public interface TaskNodeView {
    Optional<LinkID> rootNodeId();
}
