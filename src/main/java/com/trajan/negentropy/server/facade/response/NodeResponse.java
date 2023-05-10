package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNode;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class NodeResponse extends Response {
    private final TaskNode node;
    private final Task parent;
    private final Task child;

    public NodeResponse(Boolean success, TaskLink link, String message) {
        super(success, message);
        if (link != null) {
            this.node = DataContext.toDTO(link);
            this.parent = link.parent() != null ?
                    DataContext.toDTO(link.parent()) :
                    null;
            this.child = DataContext.toDTO(link.child());
        } else {
            this.node = null;
            this.parent = null;
            this.child = null;
        }
    }
}
