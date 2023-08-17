package com.trajan.negentropy.server.facade.old;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.facade.response.Response;
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
            this.node = DataContext.toDO(link);
            this.parent = link.parent() != null ?
                    DataContext.toDO(link.parent()) :
                    null;
            this.child = DataContext.toDO(link.child());
        } else {
            this.node = null;
            this.parent = null;
            this.child = null;
        }
    }
}
