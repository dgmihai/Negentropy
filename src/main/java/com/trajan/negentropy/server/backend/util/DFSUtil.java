package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.server.facade.model.id.ID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class DFSUtil {
    public static <I extends ID, T> List<T> traverseFromID(I rootId,
                                                Function<I, Stream<T>> getNextNodes,
                                                Function<T, I> getNextNode) {
        return traverseFromID(rootId, getNextNodes, getNextNode, link -> {});
    }

    public static <I extends ID, T> List<T> traverseFromID(I rootId,
                                                           Function<I, Stream<T>> getNextNodes,
                                                           Function<T, I> getNextNode,
                                                           Consumer<T> consumer) {
        List<T> results = new ArrayList<>();
        recurse(rootId, results, getNextNodes, getNextNode, consumer, true);
        return results;
    }

    private static <I extends ID, T> void recurse(I id, List<T> results,
                                Function<I, Stream<T>> getNextNodes,
                                Function<T, I> getNextNode,
                                Consumer<T> consumer,
                                boolean first) {
        if (id != null || first) {
            getNextNodes.apply(id)
                    .peek(node -> {
                        results.add(node);
                        consumer.accept(node);
                    })
                    .forEachOrdered(node -> recurse(getNextNode.apply(node), results,
                            getNextNodes, getNextNode, consumer, false));
        }
    }
}
