package com.trajan.negentropy.model.interfaces;

import java.util.List;

public interface Ancestor<T> {
    List<T> children();
}
