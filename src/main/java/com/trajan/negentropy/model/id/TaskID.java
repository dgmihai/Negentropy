package com.trajan.negentropy.model.id;

import com.trajan.negentropy.model.id.ID.TaskOrLinkID;

import java.io.Serializable;

public class TaskID extends TaskOrLinkID implements Serializable {
    public TaskID(long val) {
        super(val);
    }

    public static TaskID nil() {
        return new TaskID(-1);
    }
}