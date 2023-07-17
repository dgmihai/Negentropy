package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class DFSUtil {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
//    public static <I extends ID, T> List<T> traverseFromID(I rootId,
//                                                Function<I, Stream<T>> getNextNodes,
//                                                Function<T, I> getNextNodeID) {
//        return traverseFromID(rootId, getNextNodes, getNextNodeID, link -> {});
//    }
//
//    public static <I extends ID, T> List<T> traverseFromID(I rootId,
//                                                           Function<I, Stream<T>> getNextNodes,
//                                                           Function<T, I> getNextNodeID,
//                                                           Consumer<T> consumer) {
//        List<T> results = new ArrayList<>();
//        recurse(rootId, results, getNextNodes, getNextNodeID, consumer, true);
//        return results;
//    }

//    private static <I extends ID, T> void recurse(I id, List<T> results,
//                                Function<I, Stream<T>> getNextNodes,
//                                Function<T, I> getNextNodeID,
//                                Consumer<T> consumer,
//                                boolean first) {
//        if (id != null || first) {
//            getNextNodes.apply(id)
//                    .peek(node -> {
//                        results.add(node);
//                        consumer.accept(node);
//                    })
//                    .forEachOrdered(node -> recurse(getNextNodeID.apply(node), results,
//                            getNextNodes, getNextNodeID, consumer, false));
//        }
//    }

//    public static Stream<TaskLink> traverseTaskLinksFromID(TaskID rootId,
//                                                           Function<TaskLink, TaskID> getNextID,
//                                                           Function<TaskID, Stream<TaskLink>> getNextLinks) {
//        return traverseTaskLinksFromID(rootId, getNextID, getNextLinks, null);
//    }
//
//    public static Stream<TaskLink> traverseTaskLinksFromID(TaskID rootId,
//                                                           Function<TaskLink, TaskID> getNextID,
//                                                         Function<TaskID, Stream<TaskLink>> getNextLinks,
//                                                           Consumer<TaskLink> consumer) {
//        Stream.Builder<TaskLink> streamBuilder = Stream.builder();
//        getNextLinks.apply(rootId).forEachOrdered(link -> {
//            recurse(link, streamBuilder,getNextID, getNextLinks, consumer);
//        });
//        return streamBuilder.build();
//    }
//
//    private static void recurse(TaskLink currentLink, Stream.Builder<TaskLink> streamBuilder,
//                                Function<TaskLink, TaskID> getNextID, Function<TaskID, Stream<TaskLink>> getNextLinks,
//                                Consumer<TaskLink> consumer) {
//        streamBuilder.accept(currentLink);
//        if (consumer != null) consumer.accept(currentLink);
//
//        getNextLinks.apply(getNextID.apply(currentLink))
//                .forEachOrdered(nextLink -> recurse(
//                        nextLink, streamBuilder, getNextID, getNextLinks, consumer));
//    }

    /**
     * Performs a depth-first search (DFS) from a root TaskID and returns a list of TaskLinks with a cumulative duration
     * not exceeding a given limit.
     * <p>
     * The DFS starts from the root TaskID and uses the getNextLinks function to find the next TaskLinks from a TaskID
     * and the getNextLinkID function to find the ID from a node. The search continues until the cumulative duration of
     * the tasks exceeds the durationLimit.
     * </p>
     *
     * @param rootLink      The root TaskLink from where the search should start, and will be included.
     * @param getNextLinks  A function to retrieve a stream of child TaskLinks given a TaskID.
     * @param durationLimit The cumulative duration limit for the tasks.
     * @param consumer      A consumer function to peek at each TaskLink.
     * @return A list of tasks from the DFS, with cumulative duration not exceeding the limit.
     * @throws IllegalArgumentException if the durationLimit is non-positive.
     */
    public static Stream<TaskLink> traverseTaskLinks(TaskID rootId,
                                                     Function<TaskLink, TaskID> getNextID,
                                                     Function<TaskID, Stream<TaskLink>> getNextLinks,
                                                     Duration durationLimit,
                                                     Consumer<TaskLink> consumer,
                                                     boolean withDurationLimits) {
        Stream.Builder<TaskLink> streamBuilder = Stream.builder();
        for (TaskLink nextLink : getNextLinks.apply(rootId).toList()) {
                durationLimit = recurse(nextLink, streamBuilder, getNextID, getNextLinks, consumer,
                        durationLimit, withDurationLimits);
        }
        return streamBuilder.build();
    }

//    private static boolean recurse(TaskLink currentLink,
//                                Stream.Builder<TaskLink> streamBuilder,
//                                Function<TaskLink, TaskID> getNextID,
//                                Function<TaskID, Stream<TaskLink>> getNextLinks,
//                                Consumer<TaskLink> consumer,
//                                Duration durationLimit,
//                                boolean withDurationLimits) {
//        log.trace("Entry: Task " + currentLink.child().name());
//        log.trace("Duration limit: " + durationLimit + ", with limits: " + withDurationLimits);
//
//        TaskEntity currentTask = currentLink.child();
//        Duration currentDuration = currentTask.duration();
//        Duration remainingDuration = durationLimit;
//
//        if (withDurationLimits) {
//            if (currentTask.project()) {
//                if (durationLimit == null || durationLimit.compareTo(currentLink.projectDuration()) > 0) {
//                    remainingDuration = currentLink.projectDuration();
//                }
//            }
//            if (currentTask.block() && durationLimit != null) {
//                // TODO: Caching - this gets the estimated duration for a task
//                Duration blockDuration = traverseTaskLinks(
//                        ID.of(currentTask),
//                        getNextID,
//                        getNextLinks,
//                        durationLimit,
//                        null,
//                        true)
//                        .map(link -> link.child().duration())
//                        .reduce(currentDuration, Duration::plus);
//                remainingDuration = remainingDuration.minus(blockDuration);
//            } else {
//                remainingDuration = remainingDuration != null
//                        ? remainingDuration.minus(currentDuration)
//                        : null;
//                log.trace("Subtraction: " + remainingDuration);
//            }
//
//            if (remainingDuration != null && remainingDuration.isNegative()) {
//                return false;
//            }
//        }
//
//        streamBuilder.accept(currentLink);
//        if (consumer != null) consumer.accept(currentLink);
//
//        for (TaskLink nextLink : getNextLinks.apply(getNextID.apply(currentLink)).toList()) {
//            TaskEntity nextTask = nextLink.child();
//            log.trace("Task " + nextTask.name() + " - nextLimit: " + remainingDuration);
//             if (!recurse(nextLink, streamBuilder, getNextID, getNextLinks, consumer, remainingDuration,
//                    withDurationLimits)) {
//                 break;
//             }
//        }
//
//        return true;
//    }

    private static Duration recurse(TaskLink currentLink,
                                Stream.Builder<TaskLink> streamBuilder,
                                Function<TaskLink, TaskID> getNextID,
                                Function<TaskID, Stream<TaskLink>> getNextLinks,
                                Consumer<TaskLink> consumer,
                                Duration remainingDuration,
                                boolean withDurationLimits) {
        log.debug("Recursing at : " + currentLink.child().name() + ", " + remainingDuration + ", withDurationLimit: " + withDurationLimits);

        if (withDurationLimits) {
            if (remainingDuration != null) {
                remainingDuration = remainingDuration.minus(currentLink.child().duration());
                if (remainingDuration.isNegative()) {
                    return remainingDuration;
                }
            }

            if (currentLink.child().project()) {
                Duration projectDurationLimit = currentLink.projectDuration();
                if (remainingDuration != null && projectDurationLimit != null) {
                    remainingDuration = remainingDuration.compareTo(projectDurationLimit) < 0
                            ? remainingDuration
                            : projectDurationLimit;
                } else {
                    remainingDuration = projectDurationLimit;
                }
            }
        }

        if (consumer != null) consumer.accept(currentLink);
        streamBuilder.add(currentLink);

        if (remainingDuration == null || !remainingDuration.isZero()) {
            List<TaskLink> nextLinks = getNextLinks.apply(getNextID.apply(currentLink)).toList();
            for (TaskLink nextLink : nextLinks) {
                remainingDuration = recurse(nextLink, streamBuilder, getNextID, getNextLinks, consumer, remainingDuration, withDurationLimits);
                if (remainingDuration != null && withDurationLimits) {
                    if (remainingDuration.isNegative()) {
                        break;
                    }
                }
            }
        }
        return remainingDuration;
    }
}
