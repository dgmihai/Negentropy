package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.interfaces.Named;

public interface TagData<T extends TagData<T>> extends Data, Named {
    T name(String name);
}
