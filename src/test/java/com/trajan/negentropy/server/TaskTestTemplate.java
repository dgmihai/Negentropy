package com.trajan.negentropy.server;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.UpdateService;
import com.trajan.negentropy.server.facade.model.*;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TagID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskTestTemplate {
    @Autowired protected EntityQueryService entityQueryService;
    @Autowired protected UpdateService updateService;
    @Autowired protected QueryService queryService;
    @Autowired private DataContext dataContext;

    @Autowired protected TaskRepository taskRepository;

    protected static final LocalDateTime MARK = LocalDateTime.of(2000, 1, 1, 0, 0);

    protected static final String NULL = "";
    protected static final String ONE = "One";
    protected static final String TWO = "Two";
    protected static final String TWOONE = "TwoOne";
    protected static final String TWOTWO = "TwoTwo";
    protected static final String TWOTWOONE = "TwoTwoOne";
    protected static final String TWOTWOTWO = "TwoTwoTwo";
    protected static final String TWOTWOTHREE_AND_THREETWOTWO = "TwoTwoThreeAndThreeTwoTwo";
    protected static final String TWOTHREE = "TwoThree";
    protected static final String THREE_AND_FIVE = "ThreeAndFive";
    protected static final String THREEONE = "ThreeOne";
    protected static final String THREETWO = "ThreeTwo";
    protected static final String THREETWOONE_AND_THREETWOTHREE = "ThreeTwoOneAndThreeTwoThree";
    protected static final String THREETHREE = "ThreeThree";
    protected static final String FOUR = "Four";
    protected static final String SIX_AND_THREETWOFOUR = "SixAndThreeTwoFour";

    protected static final String REPEATONCE = "RepeatOnce";
    protected static final String REPEATSEVERAL = "RepeatSeveral";
    protected static final String REPEATINSAMEPARENT = "RepeatInSameParent";

    /* Populate test data

        TEST DATA TASK HIERARCHY TREE

            One
            Two
                TwoOne
                TwoTwo
                    TwoTwoOne
                    TwoTwoTwo
                    TwoTwoThreeAndThreeTwoTwo
                TwoThree
            ThreeAndFive
                ThreeOne
                ThreeTwo
                    ThreeTwoOneAndThreeTwoThree
                    TwoTwoThreeAndThreeTwoTwo
                    ThreeTwoOneAndThreeTwoThree
                    SixAndThreeTwoFour
                ThreeThree
            Four
            ThreeAndFive
                ThreeOne
                ThreeTwo
                    ThreeTwoOneAndThreeTwoThree
                    TwoTwoThreeAndThreeTwoTwo
                    ThreeTwoOneAndThreeTwoThree
                    SixAndThreeTwoFour
                ThreeThree
            SixAndThreeTwoFour

        TAGS

            RepeatOnce:
                ThreeAndFive
                TwoTwoThreeAndThreeTwoTwo
            RepeatSeveral:
                ThreeOne
                ThreeTwo
                    ThreeTwoOneAndThreeTwoThree
                    TwoTwoThreeAndThreeTwoTwo
                ThreeThree
                SixAndThreeTwoFour
            RepeatInSameParent:
                ThreeAndFive
                ThreeTwoOneAndThreeTwoThree
      */

    protected final Map<String, Task> tasks = new HashMap<>();
    protected final Map<Triple<String, String, Integer>, TaskNode> nodes = new HashMap<>();
    protected final Map<String, Tag> tags = new HashMap<>();

    protected final Map<String, TaskEntity> taskEntities = new HashMap<>();
    protected final Map<Triple<String, String, Integer>, TaskLink> links = new HashMap<>();
    protected final Map<String, TagEntity> tagEntities = new HashMap<>();

    protected final Map<TaskID, String> taskIds = new HashMap<>();
    protected final Map<TagID, String> tagIds = new HashMap<>();

    // Init tasks - (Task name, duration in minutes, if block)
    private void initTasks(String parent, List<Pair<Task, TaskNodeInfo>> children) {
        for (int i=0; i<children.size(); i++) {
            Task task = children.get(i).getFirst();
            TaskNodeInfo nodeInfo = children.get(i).getSecond();
            String name = task.name();
            if (!tasks.containsKey(name)) {
                tasks.put(name, updateService.createTask(new Task(null)
                                .name(name)
                                .duration(Duration.ofMinutes(1))
                                .block(task.block()))
                        .task());
            }

            TaskID childId = tasks.get(name).id();
            TaskID parentId = parent == null ?
                    null : tasks.get(parent).id();

            TaskNodeDTO freshNode = new TaskNodeDTO(
                    parentId,
                    childId,
                    i,
//                  TODO: implement importance
//                    task.importance,
                    0,
                    false,
                    false,
                    nodeInfo.cron());

            TaskNode node = updateService.insertTaskNode(freshNode).node();

            Triple<String, String, Integer> linkKey = Triple.of(
                    Objects.requireNonNullElse(parent, NULL),
                    name,
                    i);

            TaskLink link = entityQueryService.getLink(node.linkId())
                    .createdAt(MARK)
                    .scheduledFor(MARK);

            if (node.cron() != null) {
                link.scheduledFor(node.cron().next(MARK));
            }

            link = dataContext.TESTONLY_mergeLink(link);
            node = queryService.fetchNode(ID.of(link));

            nodes.put(linkKey, node);
            links.put(linkKey, entityQueryService.getLink(node.linkId()));
        }
    }

    private void initTags(String tagName, List<String> tasksToTag) {
        Tag tag = updateService.createTag(new Tag(null, tagName)).tag();
        tags.put(tagName, tag);
        for (String taskToTag : tasksToTag) {
            Task task = tasks.get(taskToTag);
            task.tags().add(tag);
            updateService.updateTask(task);
        }
    }

    private void refreshMaps() {
        for (Task task : tasks.values()) {
            tasks.put(task.name(), queryService.fetchTask(task.id()));
            taskIds.put(task.id(), task.name());
            taskEntities.put(task.name(), entityQueryService.getTask(task.id()));
        }
        for (Tag tag : tags.values()) {
            tags.put(tag.name(), queryService.fetchTag(tag.id()));
            tagIds.put(tag.id(), tag.name());
            tagEntities.put(tag.name(), entityQueryService.getTag(tag.id()));
        }
    }

    protected void assertLinkQueryResults(Iterable<Triple<String, String, Integer>> expected,
                                          Stream<TaskLink> results, boolean ordered) {
        List<Triple<String, String, Integer>> resultNameList = results
                .map(link -> Triple.of(
                        link.parent() == null ?
                                NULL :
                                link.parent().name(),
                        link.child().name(),
                        link.position()))
                .toList();

        assertTrue(ordered ?
                Iterables.elementsEqual(expected, resultNameList) :
                Iterables.size(expected) == resultNameList.size());
    }

    protected void assertTaskQueryResults(Iterable<String> expected, Stream<TaskEntity> results,
                                        boolean ordered) {
        List<String> resultNameList = results
                .peek(task -> assertTrue(task == null || taskIds.containsKey(ID.of(task))))
                .map(task -> task == null ? NULL : task.name())
                .toList();

        assertTrue(ordered ?
                Iterables.elementsEqual(expected, resultNameList) :
                Iterables.size(expected) == resultNameList.size());
    }

    protected void testFindDescendantLinks(String ancestor, TaskFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findDescendantLinks(
                ancestor == null ?
                        null :
                        tasks.get(ancestor).id(),
                filter), true);
    }

    // Testing findDescendantTasks

    protected void testFindDescendantTasks(String ancestor, TaskFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findDescendantTasks(
                ancestor == null ?
                        null :
                        tasks.get(ancestor).id(),
                filter), true);
    }

    protected Task task(String name, boolean block) {
        return new Task()
                .name(name)
//                TODO: Implement importance
//                .importance(importance)
                .block(block);
    }

    protected void init() {
        CronExpression daily = CronExpression.parse("@daily");
        CronExpression weekly = CronExpression.parse("0 0 0 */7 * ?");
        CronExpression everyWednesday = CronExpression.parse("0 0 0 * * WED");
        CronExpression onlyEvenings = CronExpression.parse("0 0 17-23 * * ?");
        CronExpression afterTheTenth = CronExpression.parse("0 0 0 10/31 * ?");
        CronExpression nextMonth = CronExpression.parse("0 0 0 1 * ?");

        initTasks(null,
                List.of(
                        Pair.of(task(ONE, false), new TaskNodeInfo()),
                        Pair.of(task(TWO, false), new TaskNodeInfo()
                                .cron(daily)),
                        Pair.of(task(THREE_AND_FIVE, false), new TaskNodeInfo()
                                .cron(weekly)),
                        Pair.of(task(FOUR, false), new TaskNodeInfo()),
                        Pair.of(task(THREE_AND_FIVE, false), new TaskNodeInfo()),
                        Pair.of(task(SIX_AND_THREETWOFOUR, false), new TaskNodeInfo()))
        );

        initTasks(TWO,
                List.of(
                        Pair.of(task(TWOONE, false), new TaskNodeInfo()),
                        Pair.of(task(TWOTWO, true), new TaskNodeInfo()
                                .cron(afterTheTenth)),
                        Pair.of(task(TWOTHREE, false), new TaskNodeInfo()
                                .cron(everyWednesday)))
        );

        initTasks(TWOTWO,
                List.of(
                        Pair.of(task(TWOTWOONE, true), new TaskNodeInfo()
                                .cron(onlyEvenings)),
                        Pair.of(task(TWOTWOTWO, true), new TaskNodeInfo()
                                .cron(daily)),
                        Pair.of(task(TWOTWOTHREE_AND_THREETWOTWO, true), new TaskNodeInfo()
                                .cron(everyWednesday)))
        );

        initTasks(THREE_AND_FIVE,
                List.of(
                        Pair.of(task(THREEONE, false), new TaskNodeInfo()
                                .cron(nextMonth)),
                        Pair.of(task(THREETWO, false), new TaskNodeInfo()
                                .cron(daily)),
                        Pair.of(task(THREETHREE, true), new TaskNodeInfo()))
        );

        initTasks(THREETWO,
                List.of(
                        Pair.of(task(THREETWOONE_AND_THREETWOTHREE, true), new TaskNodeInfo()),
                        Pair.of(task(TWOTWOTHREE_AND_THREETWOTWO, true), new TaskNodeInfo()),
                        Pair.of(task(THREETWOONE_AND_THREETWOTHREE, true), new TaskNodeInfo()),
                        Pair.of(task(SIX_AND_THREETWOFOUR, false), new TaskNodeInfo()))
        );

        initTags(REPEATONCE,
                List.of(
                        THREE_AND_FIVE,
                        TWOTWOTHREE_AND_THREETWOTWO));

        initTags(REPEATSEVERAL,
                List.of(
                        THREEONE,
                        THREETWO,
                        THREETWOONE_AND_THREETWOTHREE,
                        TWOTWOTHREE_AND_THREETWOTWO,
                        THREETHREE,
                        SIX_AND_THREETWOFOUR));

        initTags(REPEATINSAMEPARENT,
                List.of(
                        THREE_AND_FIVE,
                        THREETWOONE_AND_THREETWOTHREE));

        refreshMaps();
    }
}
