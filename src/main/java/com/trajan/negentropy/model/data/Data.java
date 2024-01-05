package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.interfaces.Named;

import java.io.Serializable;

public interface Data extends Serializable {
    default String typeName() {
        return this.getClass().getSimpleName();
    }

    interface PersistedDataDO<I extends ID> extends Data, Named {
        I id();
    }
}
