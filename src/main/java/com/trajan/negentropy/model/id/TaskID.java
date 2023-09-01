package com.trajan.negentropy.model.id;

import com.trajan.negentropy.model.id.ID.TaskOrLinkID;

public class TaskID extends ID implements TaskOrLinkID {
    public TaskID(long val) {
        super(val);
    }

    public static TaskID nil() {
        return new TaskID(-1);
    }
}