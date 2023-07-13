package com.trajan.negentropy.server.facade.model.interfaces;

import java.util.List;

public interface Ancestor<T> {
    List<T> children();
}
