package com.trajan.negentropy.server.backend.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trajan.negentropy.model.interfaces.Ancestor;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.function.Function;

public class AncestorOrderingUtil<I, T extends Ancestor<T>> {
    private final Function<T, I> getIdentifier; // May return null

    public AncestorOrderingUtil(Function<T, I> getIdentifier) {
        this.getIdentifier = getIdentifier;
    }

    public T rearrangeAncestor(@NotNull T existing, @NotNull T target) {
        Multimap<I, T> existingMultimap = ArrayListMultimap.create();
        buildEntityMap(existing, existingMultimap);

        Map<T, List<T>> parentChildMap = new HashMap<>();
        T newRoot = rearrange(target, existingMultimap, parentChildMap, new HashMap<>(), new HashSet<>());

        for (Map.Entry<T, List<T>> entry : parentChildMap.entrySet()) {
            List<T> children = entry.getKey().children();
            children.clear();
            children.addAll(entry.getValue());
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

    private T rearrange(T target, Multimap<I, T> existingMultimap, Map<T, List<T>> parentChildMap,
                        Map<I, Integer> idCountMap, Set<I> newDuplicates) {
        I entityId = getIdentifier.apply(target);
        int targetCount = idCountMap.getOrDefault(entityId, 0);
        List<T> existingEntities = new ArrayList<>(existingMultimap.get(entityId));

        T rearranged;
        if (existingEntities.size() > targetCount && !newDuplicates.contains(entityId)) {
            rearranged = existingEntities.get(targetCount);
        } else {
            rearranged = target;
            newDuplicates.add(entityId);
        }

        idCountMap.put(entityId, targetCount + 1);

        List<T> children = parentChildMap.computeIfAbsent(rearranged, k -> new ArrayList<>());
        for (T targetChild : target.children()) {
            T rearrangedChild = rearrange(targetChild, existingMultimap, parentChildMap, idCountMap, newDuplicates);
            if (rearrangedChild != null) {
                children.add(rearrangedChild);
            }
        }

        return rearranged;
    }
}
