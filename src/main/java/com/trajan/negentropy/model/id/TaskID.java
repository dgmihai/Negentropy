package com.trajan.negentropy.model.id;

public class TaskID extends ID {
    public TaskID(long val) {
        super(val);
    }

    public static TaskID nil() {
        return new TaskID(-1);
    }
}