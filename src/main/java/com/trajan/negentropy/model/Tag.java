package com.trajan.negentropy.model;

import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.data.TagData;
import com.trajan.negentropy.model.id.TagID;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class Tag implements TagData<Tag>, PersistedDataDO<TagID> {
    private TagID id;
    private String name;

    @Override
    public Tag name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return "Tag(" + this.id + ", name=" + this.name + ")";
    }
}