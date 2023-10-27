package com.trajan.negentropy.server.backend.util;

import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.interfaces.Ancestor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public class DFSUtil {
    public static <T extends Ancestor<T>> List<T> traverse(T root) {
        List<T> nodes = new ArrayList<>();
        recurse(root, nodes);
        return nodes;
    }

    private static <T extends Ancestor<T>> void recurse(T node, List<T> nodes) {
        nodes.add(node);
        for (T child : node.children()) {
            recurse(child, nodes);
        }
    }

    public static <T extends Ancestor<T>> void forEach(T root, Consumer<T> consumer) {
        recurse(root, consumer);
    }

    private static <T extends Ancestor<T>> void recurse(T node, Consumer<T> consumer) {
        consumer.accept(node);
        for (T child : node.children()) {
            recurse(child, consumer);
        }
    }

    public static <T extends Ancestor<T>> T traverseTo(int position, T root) {
        return recurseTo(position, root);
    }

    private static <T extends Ancestor<T>> T recurseTo(int position, T node) {
        position--;
        if (position == 0) return node;
        for (T child : node.children()) {
            recurseTo(position, child);
        }
        return null;
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
     * @param consumer      A consumer function to peek at each TaskLink.
     * @return A list of tasks from the DFS, with cumulative duration not exceeding the limit.
     * @throws IllegalArgumentException if the durationLimit is non-positive.
     */
    public static Stream<TaskLink> traverseTaskLinks(TaskID rootId,
                                                     Function<TaskLink, TaskID> getNextID,
                                                     Function<TaskID, Stream<TaskLink>> getNextLinks,
                                                     Consumer<TaskLink> consumer) {
        Stream.Builder<TaskLink> streamBuilder = Stream.builder();
        for (TaskLink nextLink : getNextLinks.apply(rootId).toList()) {
            recurseTaskLinks(nextLink, streamBuilder, null, getNextID, getNextLinks, consumer);
        }
        return streamBuilder.build();
    }

    public static Map<TaskLink, List<TaskLink>> traverseTaskLinksAsAdjacencyMap
            (TaskID rootId,
             Function<TaskLink, TaskID> getNextID,
             Function<TaskID, Stream<TaskLink>> getNextLinks,
             Consumer<TaskLink> consumer) {
        Map<TaskLink, List<TaskLink>> adjacencyMap = new LinkedHashMap<>();
        for (TaskLink nextLink : getNextLinks.apply(rootId).toList()) {
            recurseTaskLinks(nextLink, null, adjacencyMap, getNextID, getNextLinks, consumer);
        }
        return adjacencyMap;
    }

    private static void recurseTaskLinks(
            TaskLink currentLink,
            Stream.Builder<TaskLink> streamBuilder,
            Map<TaskLink, List<TaskLink>> adjacencyMap,
            Function<TaskLink, TaskID> getNextID,
            Function<TaskID, Stream<TaskLink>> getNextLinks,
            Consumer<TaskLink> consumer) {
        log.trace("Recursing at : " + currentLink.child().name());
        if (consumer != null) consumer.accept(currentLink);

        if (streamBuilder != null) {
            streamBuilder.add(currentLink);
        }
        if (adjacencyMap != null) {
            adjacencyMap.put(currentLink, new LinkedList<>());
        }

        List<TaskLink> nextLinks = getNextLinks.apply(getNextID.apply(currentLink)).toList();
        for (TaskLink nextLink : nextLinks) {
            recurseTaskLinks(nextLink, streamBuilder, adjacencyMap, getNextID,
                    getNextLinks, consumer);

            if (adjacencyMap != null) {
                adjacencyMap.get(currentLink).add(nextLink);
            }
        }
    }
}
