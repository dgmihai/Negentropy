package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.facade.model.Tag;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class TagResponse extends Response {
    private final Tag tag;

    public TagResponse(Boolean success, TagEntity tag, String message) {
        super(success, message);
        this.tag = tag == null ?
                null :
                DataContext.toDTO(tag);
    }
}
