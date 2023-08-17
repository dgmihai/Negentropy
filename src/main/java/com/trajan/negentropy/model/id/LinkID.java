package com.trajan.negentropy.model.id;

public class LinkID extends ID {
    public LinkID(long val) {
        super(val);
    }

    public static LinkID nil() {
        return new LinkID(-1);
    }
}