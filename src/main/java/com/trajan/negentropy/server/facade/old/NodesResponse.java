package com.trajan.negentropy.server.facade.old;

import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.server.facade.response.Response;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.stream.Stream;

@Accessors(fluent = true)
@Getter
public class NodesResponse extends Response {
    private final Stream<TaskNode> nodes;

    public NodesResponse(Boolean success, Stream<TaskNode> nodes, String message) {
        super(success, message);
        this.nodes = nodes;
    }
}
