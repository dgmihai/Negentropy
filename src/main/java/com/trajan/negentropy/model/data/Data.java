package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.id.ID;

public interface Data {
    interface PersistedDataDO<I extends ID> extends Data {
        I id();
    }
}
