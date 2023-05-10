package com.trajan.negentropy.server.facade.model.filter;

import com.trajan.negentropy.server.facade.model.id.TagID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

@Accessors(fluent = true)
@NoArgsConstructor
@Getter
@Setter
public class TaskFilter {
    private String name = "";
    private Set<TagID> includedTagIds = new HashSet<>();
    private Set<TagID> excludedTagIds = new HashSet<>();
    private Integer importanceThreshold;
}