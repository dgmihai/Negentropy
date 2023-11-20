package com.trajan.negentropy.model.id;

import com.trajan.negentropy.model.id.ID.TaskOrLinkID;

public class LinkID extends TaskOrLinkID {
    public LinkID(long val) {
        super(val);
    }

    public static LinkID nil() {
        return new LinkID(-1);
    }
}