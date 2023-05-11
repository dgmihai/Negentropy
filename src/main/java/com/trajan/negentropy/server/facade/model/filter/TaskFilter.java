package com.trajan.negentropy.server.facade.model.filter;

import com.trajan.negentropy.server.facade.model.id.TagID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Set;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
public class TaskFilter {
    private String name;
    private Set<TagID> includedTagIds;
    private Set<TagID> excludedTagIds;
    private Integer importanceThreshold;
}