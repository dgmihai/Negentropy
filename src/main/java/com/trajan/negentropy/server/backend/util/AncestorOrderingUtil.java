package com.trajan.negentropy.server.backend.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trajan.negentropy.model.interfaces.Ancestor;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class AncestorOrderingUtil<I, T extends Ancestor<T>> {
    private final Function<T, I> getIdentifier; // May return null
    @Getter
    private final Multimap<I, T> newDuplicates = ArrayListMultimap.create();
    @Getter
    private final Map<T, List<T>> parentChildMap = new HashMap<>();
    public BiFunction<T, T, T> onAdd = (parent, child) -> child;

    public AncestorOrderingUtil(Function<T, I> getIdentifier) {
        this.getIdentifier = getIdentifier;
    }

    public T rearrangeAncestor(@NotNull T existing, @NotNull T target) {
        Multimap<I, T> existingMultimap = ArrayListMultimap.create();
        newDuplicates.clear();
        parentChildMap.clear();

        buildEntityMap(existing, existingMultimap);

        T newRoot = rearrange(target, existingMultimap, new HashMap<>());

        for (Map.Entry<T, List<T>> entry : parentChildMap.entrySet()) {
            List<T> children = entry.getKey().children();
            children.clear();
            for (T child : entry.getValue()) {
                children.add(onAdd.apply(entry.getKey(), child));
            }
        }

        return newRoot;
    }

    private void buildEntityMap(T ancestor, Multimap<I, T> multimap) {
        I entityId = getIdentifier.apply(ancestor);
        if (entityId != null) {
            multimap.put(getIdentifier.apply(ancestor), ancestor);
            for (T child : ancestor.children()) {
                buildEntityMap(child, multimap);
            }
        }
    }

    private T rearrange(T target, Multimap<I, T> existingMultimap,
                        Map<I, Integer> idCountMap) {
        I entityId = getIdentifier.apply(target);

        int targetCount = idCountMap.getOrDefault(entityId, 0);
        List<T> existingEntities = new ArrayList<>(existingMultimap.get(entityId));

        T rearranged;
        if (existingEntities.size() > targetCount && !newDuplicates.containsKey(entityId)) {
            rearranged = existingEntities.get(targetCount);
        } else {
            rearranged = target;
            newDuplicates.put(entityId, target);
        }

        idCountMap.put(entityId, targetCount + 1);

        List<T> children = parentChildMap.computeIfAbsent(rearranged, k -> new ArrayList<>());
        for (T targetChild : target.children()) {
            T rearrangedChild = rearrange(targetChild, existingMultimap, idCountMap);
            if (rearrangedChild != null) {
                children.add(rearrangedChild);
            }
        }

        return rearranged;
    }
}
