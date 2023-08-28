package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class DFSUtil {
    public static <T extends Ancestor<T>> Deque<T> traverse(T root) {
        Deque<T> nodes = new LinkedList<>();
        recurse(root, nodes);
        return nodes;
    }

    private static <T extends Ancestor<T>> void recurse(T node, Deque<T> nodes) {
        nodes.add(node);
        for (T child : node.children()) {
            recurse(child, nodes);
        }
    }

    /**
     * Performs a depth-first search (DFS) from a root TaskID and returns a list of TaskLinks with a cumulative duration
     * not exceeding a given limit.
     * <p>
     * The DFS starts from the root TaskID and uses the getNextLinks function to find the next TaskLinks from a TaskID
     * and the getNextLinkID function to find the ID from a node. The search continues until the cumulative duration of
     * the tasks exceeds the durationLimit.
     * </p>
     *
     * @param rootId        The changes of the root TaskLink from where the search should start, and will be included.
     * @param getNextID     A function to retrieve the ID of the next task link in the tree.
     * @param getNextLinks  A function to retrieve a stream of succeeding TaskLinks given a TaskID.
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
            durationLimit = recurseTaskLinks(nextLink, streamBuilder, getNextID, getNextLinks, consumer,
                    durationLimit, withDurationLimits);
        }
        return streamBuilder.build();
    }

    private static Duration recurseTaskLinks(
            TaskLink currentLink,
            Stream.Builder<TaskLink> streamBuilder,
            Function<TaskLink, TaskID> getNextID,
            Function<TaskID, Stream<TaskLink>> getNextLinks,
            Consumer<TaskLink> consumer,
            Duration remainingDuration,
            boolean withDurationLimits) {
        log.trace("Recursing at : " + currentLink.child().name() + ", " + remainingDuration + ", withDurationLimit: " + withDurationLimits);

        Duration continuedDuration = null;
        if (withDurationLimits) {
            if (remainingDuration != null) {
                continuedDuration = remainingDuration.minus(currentLink.child().duration());
                if (continuedDuration.isNegative()) {
                    if (!currentLink.child().required()) {
                        return continuedDuration;
                    }
                }
            }

            if (currentLink.child().project()) {
                Duration projectDurationLimit = currentLink.projectDuration();
                if (continuedDuration != null && projectDurationLimit != null) {
                    continuedDuration = continuedDuration.compareTo(projectDurationLimit) < 0
                            ? continuedDuration
                            : projectDurationLimit;
                } else {
                    continuedDuration = projectDurationLimit;
                }
            }
        }

        if (consumer != null) consumer.accept(currentLink);

        if (streamBuilder != null) {
            streamBuilder.add(currentLink);
        }

        if (continuedDuration == null || !continuedDuration.isZero()) {
            List<TaskLink> nextLinks = getNextLinks.apply(getNextID.apply(currentLink)).toList();
            for (TaskLink nextLink : nextLinks) {
                continuedDuration = recurseTaskLinks(nextLink, streamBuilder, getNextID, getNextLinks, consumer, remainingDuration, withDurationLimits);

                if (continuedDuration != null && withDurationLimits && nextLink.child().required()) {
                    if (continuedDuration.isNegative()) {
                        break;
                    }
                }
            }
        }

        return continuedDuration;
    }
}