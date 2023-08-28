package com.trajan.negentropy.model.data;

import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.interfaces.Named;

public interface Data {
    String typeName();

    interface PersistedDataDO<I extends ID> extends Data, Named {
        I id();
    }
}
