package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.id.TaskID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class DFS {
    public static List<TaskLink> getLinks(TaskID rootId,
                                          Function<TaskID, Stream<TaskLink>> getNextLinks,
                                          Function<TaskLink, TaskID> getNextLink) {
        return getLinks(rootId, getNextLinks, getNextLink, link -> {});
    }

    public static List<TaskLink> getLinks(TaskID rootId,
                                          Function<TaskID, Stream<TaskLink>> getNextLinks,
                                          Function<TaskLink, TaskID> getNextLink,
                                          Consumer<TaskLink> consumer) {
        List<TaskLink> results = new ArrayList<>();
        recurse(rootId, results, getNextLinks, getNextLink, consumer, true);
        return results;
    }

    private static void recurse(TaskID id, List<TaskLink> results,
                                Function<TaskID, Stream<TaskLink>> getNextLinks,
                                Function<TaskLink, TaskID> getNextLink,
                                Consumer<TaskLink> consumer,
                                boolean first) {
        if (id != null || first) {
            getNextLinks.apply(id)
                    .peek(link -> {
                        results.add(link);
                        consumer.accept(link);
                    })
                    .forEachOrdered(link -> recurse(getNextLink.apply(link), results,
                            getNextLinks, getNextLink, consumer, false));
        }
    }
}
