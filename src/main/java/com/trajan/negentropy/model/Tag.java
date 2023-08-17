package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.TagData;
import com.trajan.negentropy.model.id.TagID;
import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Accessors(fluent = true)
@EqualsAndHashCode
public class Tag implements TagData<Tag>, PersistedDataDO<TagID> {
    private TagID id;
    private String name;

    @Override
    public Tag name(String name) {
        this.name = name;
        return this;
    }
}