package com.trajan.negentropy.server;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Tag;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.data.Data.PersistedDataDO;
import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;
import com.trajan.negentropy.model.filter.TaskNodeTreeFilter;
import com.trajan.negentropy.model.id.ID;
import com.trajan.negentropy.model.id.ID.ChangeID;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.InsertIntoChange;
import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.EntityQueryService;
import com.trajan.negentropy.server.backend.NetDurationService;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import com.trajan.negentropy.server.facade.ChangeService;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import com.trajan.negentropy.util.TestServices;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
@Transactional
public class TaskTestTemplate {
    @Autowired protected EntityQueryService entityQueryService;
    @Autowired protected ChangeService changeService;
    @Autowired protected QueryService queryService;
    @Autowired protected NetDurationService netDurationService;
    @Autowired private DataContext dataContext;
    @Autowired protected TestServices testServices;

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

    protected static final CronExpression daily = CronExpression.parse("@daily");
    protected static final CronExpression weekly = CronExpression.parse("0 0 0 */7 * ?");
    protected static final CronExpression everyWednesday = CronExpression.parse("0 0 0 * * WED");
    protected static final CronExpression onlyEvenings = CronExpression.parse("0 0 17-23 * * ?");
    protected static final CronExpression afterTheTenth = CronExpression.parse("0 0 0 10/31 * ?");
    protected static final CronExpression nextMonth = CronExpression.parse("0 0 0 1 * ?");

    /* Populate test changeRelevantDataMap

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

        TAGS_COMBO

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

    protected Task persistTask(Task task) {
        Change persist = new PersistChange<>(task);
        ChangeID id = persist.id();

        DataMapResponse response = changeService.execute(Request.of(persist));
        log.debug("Persisted task: " + response.changeRelevantDataMap().getFirst(id));
        return (Task) response.changeRelevantDataMap().getFirst(id);
    }

    protected Task createTask(String name) {
        return persistTask(new Task().name(name));
    }

    protected PersistedDataDO<?> execute(Change change) {
        ChangeID id = change.id();

        DataMapResponse response = changeService.execute(Request.of(change));
        assertTrue(response.success());

        return response.changeRelevantDataMap().getFirst(id);
    }

    protected Task mergeTask(Task task) {
        return (Task) execute(new MergeChange<>(task));
    }

    protected TaskNode persistTaskNode(TaskNodeDTO taskNodeDTO) {
        return (TaskNode) execute(new PersistChange<>(taskNodeDTO));
    }

    protected void validateNodes(Stream<TaskNode> nodes, List<Object> expected) {
        List<TaskNode> nodeList = nodes
                .peek(node -> System.out.println("Validate nodes peek: child=" + node.child()))
                .toList();
        System.out.println("EXPECTED: " + expected);
        System.out.println("ACTUAL: " + nodeList.stream().map(node -> node.task().name()).toList());
        TaskID parentId = nodeList.get(0).parentId();
        for (int i=0; i<nodeList.size(); i++) {
            TaskNode node = nodeList.get(i);
            Task task;
            Object obj = expected.get(i);
            if (obj instanceof Task t) {
                task = t;
            } else if (obj instanceof String s) {
                task = tasks.get(s);
            } else {
                throw new RuntimeException();
            }

            assertEquals(node.position(), i);
            assertEquals(task.id(), node.child().id());
            assertEquals(parentId, node.parentId());
        }
    }

    protected void initTasks(String parent, List<Pair<Task, TaskNodeDTO>> children) {
        for (int i=0; i<children.size(); i++) {
            Task task = children.get(i).getFirst();
            TaskNodeDTO nodeDTO = children.get(i).getSecond();
            String name = task.name();

            if (!tasks.containsKey(name)) {
                if (task.duration() == null) {
                    task.duration(Duration.ofMinutes(1));
                }

                log.debug("Persisting task {}", name);
                tasks.put(name, persistTask(task));
            }

            TaskID childId = tasks.get(task.name()).id();
            TaskID parentId = parent == null ?
                    null : tasks.get(parent).id();

            TaskNodeDTO freshNode = new TaskNodeDTO(
                    parentId,
                    childId,
                    i,
                    false,
//                  TODO: implement importance
//                    task.importance,
                    0,
                    nodeDTO.completed(),
                    false,
                    nodeDTO.cron(),
                    Objects.requireNonNullElse(nodeDTO.projectDuration(), Duration.ZERO));

            Change insertInto = new InsertIntoChange(freshNode, parentId, InsertLocation.LAST);
            ChangeID id = insertInto.id();

            DataMapResponse response = changeService.execute(Request.of(insertInto));
            TaskNode node = (TaskNode) response.changeRelevantDataMap().getFirst(id);

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

    protected void initTags(String tagName, List<String> tasksToTag) {
        Tag tag = queryService.fetchTagByName(tagName);
        if (tag == null) {
            Change persistTag = new PersistChange<>(new Tag(null, tagName));
            tag = (Tag) changeService.execute(Request.of(persistTag))
                    .changeRelevantDataMap().getFirst(persistTag.id());
        }
        tags.put(tagName, tag);
        for (String taskToTag : tasksToTag) {
            Task task = tasks.get(taskToTag);
            Set<Tag> tags = new HashSet<>(task.tags());
            tags.add(tag);
            task.tags(tags);
            changeService.execute(Request.of(new MergeChange<>(task)));
        }
    }

    protected void refreshMaps() {
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

    protected void testFindDescendantLinks(String ancestor, TaskNodeTreeFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findDescendantLinks(
                ancestor == null ?
                        null :
                        tasks.get(ancestor).id(),
                filter), true);
    }

    // Testing findDescendantTasks

    protected void testFindDescendantTasks(String ancestor, TaskNodeTreeFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findDescendantTasks(
                ancestor == null ?
                        null :
                        tasks.get(ancestor).id(),
                filter), true);
    }

    protected void init() {
        initTasks(
                null,
                List.of(Pair.of(
                                new Task()
                                        .name(ONE)
                                        .required(false),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWO)
                                        .required(false)
                                        .project(true),
                                new TaskNodeDTO()
                                        .cron(daily)
                                        .projectDuration(Duration.ofMinutes(1))
                        ), Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .required(false)
                                        .project(true),
                                new TaskNodeDTO()
                                        .cron(weekly)
                                        .projectDuration(Duration.ofMinutes(1))
                        ), Pair.of(
                                new Task()
                                        .name(FOUR)
                                        .required(false)
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofSeconds(1))
                        ),Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .required(false)
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofMinutes(5))
                                        .completed(true)
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR)
                                        .required(false),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                TWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOONE)
                                        .required(false),
                                new TaskNodeDTO()
                                        .completed(true)
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWO)
                                        .required(true)
                                        .project(false),
                                new TaskNodeDTO()
                                        .cron(afterTheTenth)
                                        .projectDuration(Duration.ofMinutes(5))
                        ), Pair.of(
                                new Task()
                                        .name(TWOTHREE)
                                        .required(false),
                                new TaskNodeDTO()
                                        .cron(everyWednesday)
                        )
                )
        );

        initTasks(
                TWOTWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOTWOONE)
                                        .required(true),
                                new TaskNodeDTO()
                                        .cron(onlyEvenings)
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTWO)
                                        .required(true),
                                new TaskNodeDTO()
                                        .cron(daily)
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .required(true),
                                new TaskNodeDTO()
                                        .cron(everyWednesday)
                        )
                )
        );

        initTasks(
                THREE_AND_FIVE,
                List.of(Pair.of(
                                new Task()
                                        .name(THREEONE)
                                        .required(false),
                                new TaskNodeDTO()
                                        .cron(nextMonth)
                        ), Pair.of(
                                new Task()
                                        .name(THREETWO)
                                        .required(false),
                                new TaskNodeDTO()
                                        .cron(daily)
                        ), Pair.of(
                                new Task()
                                        .name(THREETHREE)
                                        .required(true),
                                new TaskNodeDTO()
                                        .completed(true)
                        )
                )
        );

        initTasks(
                THREETWO,
                List.of(Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .required(true),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .required(true),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .required(true),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR)
                                        .required(false),
                                new TaskNodeDTO()
                        )
                )
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
