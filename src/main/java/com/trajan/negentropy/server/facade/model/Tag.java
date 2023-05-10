package com.trajan.negentropy.server.facade.model;

import com.trajan.negentropy.server.facade.model.id.TagID;

public record Tag(
        TagID id,
        String name)
{ }
