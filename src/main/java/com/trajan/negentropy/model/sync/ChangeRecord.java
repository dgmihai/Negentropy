package com.trajan.negentropy.model.sync;

public interface ChangeRecord <T> {
    T data();

    enum ChangeRecordType {
        MERGE,
        PERSIST,
        DELETE
    }

    enum ChangeRecordDataType {
        TASK,
        LINK,
        TAG
    }
}