package com.trajan.negentropy.server.backend;

import com.google.common.collect.Iterables;
import com.trajan.negentropy.server.TaskTestTemplate;
import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.entity.TaskEntity;
import com.trajan.negentropy.server.backend.entity.TaskLink;
import com.trajan.negentropy.server.facade.model.filter.TaskFilter;
import com.trajan.negentropy.server.facade.model.id.ID;
import com.trajan.negentropy.server.facade.model.id.TaskID;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Transactional
public class EntityQueryServiceTest extends TaskTestTemplate {

    @BeforeAll
    void setUp() {
        init();
    }

    private TaskID getTaskID(String taskName) {
        return ID.of(taskEntities.get(taskName));
    }
    
    @Test
    public void testGetTask() {
        TaskEntity task = taskEntities.get(ONE);
        TaskEntity result = entityQueryService.getTask(ID.of(task));

        assertEquals(task.id(), result.id());
        assertEquals(task.name(), result.name());
    }

    @Test
    public void testGetNonexistentTask() {
        TaskEntity taskEntity = (TaskEntity) new TaskEntity()
                .id(-1L);
        assertThrows(NoSuchElementException.class,
                () -> entityQueryService.getTask(ID.of(taskEntity)));
    }

    @Test
    public void testGetLink() {
        TaskLink link = links.get(Triple.of(NULL, ONE, 0));
        TaskLink result = entityQueryService.getLink(ID.of(link));

        assertEquals(link.id(), result.id());
        assertEquals(link.parent(), result.parent());
        assertEquals(link.child(), result.child());
        assertEquals(link.position(), result.position());
        assertEquals(link.importance(), result.importance());
    }

    @Test
    public void testGetNonexistentLink() {
        TaskLink link = (TaskLink) new TaskLink()
                .id(-1L);
        assertThrows(NoSuchElementException.class,
                () -> entityQueryService.getLink(ID.of(link)));
    }

    @Test
    public void testGetLinkEnsureDistinctPositions() {
        TaskLink link1 = links.get(Triple.of(NULL, THREE_AND_FIVE, 2));
        TaskLink link2 = links.get(Triple.of(NULL, THREE_AND_FIVE, 4));

        TaskLink result1 = entityQueryService.getLink(ID.of(link1));
        TaskLink result2 = entityQueryService.getLink(ID.of(link2));

        assertNotEquals(result1.id(), result2.id());
        assertEquals(result1.parent(), result2.parent());
        assertEquals(result1.child(), result2.child());
        assertNotEquals(result1.position(), result2.position());
        // TODO: Priority
        // assertEquals(result1.importance(), result2.importance());
    }
    
    // Testing findTasks

    @Override
    protected void assertTaskQueryResults(Iterable<String> expected, Stream<TaskEntity> results,
                                        boolean ordered) {
        List<String> resultNameList = results
                .peek(task -> assertTrue(task == null || taskEntities.containsValue(task)))
                .map(task -> task == null ? NULL : task.name())
                .toList();

        assertTrue(ordered ?
                Iterables.elementsEqual(expected, resultNameList) :
                Iterables.size(expected) == resultNameList.size());
    }

    @Override
    protected void assertLinkQueryResults(Iterable<Triple<String, String, Integer>> expected, Stream<TaskLink> results,
                                          boolean ordered) {
        List<Triple<String, String, Integer>> resultNameList = results
                .peek(link -> {
                    System.out.println("PEEK: " + link);
                    assertTrue(links.containsValue(link));
                })
                .map(link -> Triple.of(
                        link.parent() == null ?
                                NULL :
                                link.parent().name(),
                        link.child().name(),
                        link.position()))
                .toList();

        System.out.println("EXPECTED: " + expected);
        System.out.println("ACTUAL:   " + resultNameList);

        assertTrue(ordered ?
                Iterables.elementsEqual(expected, resultNameList) :
                Iterables.size(expected) == resultNameList.size());
    }

    private TaskFilter createFilter(String name, Set<String> includedTags, Set<String> excludedTags) {
        TaskFilter filter = new TaskFilter();
        if (name != null) filter.name(name);
        if (includedTags != null) {
            filter.includedTagIds(includedTags.stream()
                    .map(s -> tags.get(s).id())
                    .collect(Collectors.toSet()));
        }
        if (excludedTags != null) {
            filter.excludedTagIds(excludedTags.stream()
                    .map(s -> tags.get(s).id())
                    .collect(Collectors.toSet()));
        }
        // TODO: Properly test outer join of tags
        filter.innerJoinIncludedTags(true);
        return filter;
    }

    private void testFindTasks(String name, Set<String> includedTags, Set<String> excludedTags,
                               Iterable<String> expectedResults) {
        Stream<TaskEntity> results = entityQueryService.findTasks(createFilter(
                name,
                includedTags,
                excludedTags));

        assertTaskQueryResults(expectedResults, results, false);
    }

    @Test
    public void testFindTasksFilterByNameFullMatch() {
        String name = THREE_AND_FIVE;

        List<String> expectedResults = List.of(
                THREE_AND_FIVE);

        testFindTasks(
                name,
                null,
                null,
                expectedResults);
    }

    @Test
    public void testFindTasksFilterByNamePartialMatch() {
        String name = "ThreeTwoOne";

        List<String> expectedResults = List.of(
                THREETWOONE_AND_THREETWOTHREE);

        testFindTasks(
                name,
                null,
                null,
                expectedResults);
    }

    @Test
    public void testFindTasksFilterByNameMultipleResults() {
        String name = ONE;

        List<String> expectedResults = List.of(
                ONE,
                TWOONE,
                TWOTWOONE,
                THREEONE,
                THREETWOONE_AND_THREETWOTHREE);

        testFindTasks(
                name,
                null, 
                null,
                expectedResults);
  }

    @Test
    public void testFindTasksFilterByIncludedTag() {
        Set<String> includedTags = Set.of(
                REPEATINSAMEPARENT);

        Iterable<String> expectedResults = List.of(
                THREE_AND_FIVE,
                THREETWOONE_AND_THREETWOTHREE
        );
        
        testFindTasks(
                null,
                includedTags,
                null,
                expectedResults);
    }

    @Test
    public void testFindTasksFilterByIncludedTags() {
        Set<String> includedTags = Set.of(
                REPEATONCE,
                REPEATINSAMEPARENT
        );

        Iterable<String> expectedResults = List.of(
                THREE_AND_FIVE
        );

        testFindTasks(
                null,
                includedTags,
                null,
                expectedResults);
    }

    @Test
    public void testFindTasksFilterByExcludedTag() {
        Set<String> excludedTags = Set.of(
                REPEATSEVERAL
        );

        Iterable<String> expectedResults = List.of(
                ONE,
                TWO,
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTHREE,
                FOUR,
                THREE_AND_FIVE);

        testFindTasks(
                null,
                null,
                excludedTags,
                expectedResults);
    }
    
    @Test
    public void testFindTasksFilterByExcludedTags() {
        Set<String> excludedTags = Set.of(
                REPEATSEVERAL,
                REPEATINSAMEPARENT
        );

        Iterable<String> expectedResults = List.of(
                ONE,
                TWO,
                TWOONE,
                TWOTWO, 
                TWOTWOONE,
                TWOTWOTWO,
                TWOTHREE,
                FOUR);

        testFindTasks(
                null,
                null,
                excludedTags,
                expectedResults);
    }

    @Test
    public void testFindTasksWithNoMatchingName() {        
        testFindTasks(
                "Nonexistent",
                null,
                null,
                List.of());
    }

    @Test
    public void testFindTasksWithNoMatchingIncludedTag() {
        TagEntity tagEntity = (TagEntity) new TagEntity()
                .id(-1L);
        TaskFilter filter = new TaskFilter()
                .includedTagIds(Set.of(ID.of(tagEntity)));

        assertThrows(NoSuchElementException.class, () ->
                entityQueryService.findTasks(filter).findAny().isEmpty());
    }

    @Test
    public void testFindTasksWithMultipleFilters() {
        String name = TWO;

        Set<String> includedTags = Set.of(
                REPEATSEVERAL
        );

        Set<String> excludedTags = Set.of(
                REPEATINSAMEPARENT
        );

        Iterable<String> expectedResults = List.of(
                THREETWO,
                TWOTWOTHREE_AND_THREETWOTWO,
                SIX_AND_THREETWOFOUR);

        testFindTasks(
                name,
                includedTags,
                excludedTags,
                expectedResults);
    }

    @Test
    public void testFindTasksWithEmptyFilters() {
        testFindTasks(
                null,
                null,
                null,
                taskEntities.keySet());
    }
    
    // Testing findChildCount
    
    private void testFindChildCount(String parent, TaskFilter filter, int expected) {
        int count = entityQueryService.findChildCount(getTaskID(parent), filter);
        assertEquals(expected, count);
    }

    @Test
    public void testFindChildCountOfTaskWithChildren() {
        testFindChildCount(TWOTWO, null, 3);
    }

    @Test
    public void testFindChildCountOfTaskWithNestedChildren() {
        testFindChildCount(TWO, null, 3);
    }

    @Test
    public void testFindChildCountWithFilterOfTaskWithChildren() {
        TaskFilter filter = createFilter(ONE, null, null);
        testFindChildCount(TWO, filter, 1);
    }
    
    @Test
    public void testFindChildCountOfTaskWithoutChildren() {
        testFindChildCount(FOUR, null, 0);
    }

    @Test
    public void testFindChildCountWithFilterOfTaskWithoutChildren() {
        TaskFilter filter = createFilter(ONE, null, null);
        testFindChildCount(FOUR, filter, 0);
    }


    @Test
    public void testFindChildCountWithFilterOfTaskWithChildrenAllFilteredOut() {
        TaskFilter filter = createFilter(FOUR, null, null);
        testFindChildCount(TWO, filter, 0);
    }
    
    // Testing hasChildren

    private void testHasChildren(String parent, TaskFilter filter, boolean expected) {
        assertEquals(expected, entityQueryService.hasChildren(getTaskID(parent), filter));
    }
    
    @Test
    public void testHasChildrenOfTaskWithChildren() {
        testHasChildren(TWO, null, true);
    }

    @Test
    public void testHasChildrenOfTaskWithNestedChildren() {
        testHasChildren(TWO, null, true);
    }

    @Test
    public void testHasChildrenWithFilterOfTaskWithChildren() {
        TaskFilter filter = createFilter(ONE, null, null);
        testHasChildren(TWO, filter, true);
    }

    @Test
    public void testHasChildrenOfTaskWithoutChildren() {
        testHasChildren(FOUR, null, false);
    }

    @Test
    public void testHasChildrenWithFilterOfTaskWithoutChildren() {
        TaskFilter filter = createFilter(ONE, null, null);
        testHasChildren(FOUR, filter, false);
    }


    @Test
    public void testHasChildrenWithFilterOfTaskWithChildrenAllFilteredOut() {
        TaskFilter filter = createFilter(FOUR, null, null);
        testHasChildren(TWO, filter, false);
    }

    // Testing findChildLinks
    
    private void testFindChildLinks(String parent, TaskFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findChildLinks(
                getTaskID(parent), filter), true);
    }

    @Test
    public void testFindChildLinksOfTaskWithChildren() {
        String parent = TWO;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWO, TWOONE, 0),
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWO, TWOTHREE, 2));

        testFindChildLinks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildLinksOfRoot() {
        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(NULL, ONE, 0),
                Triple.of(NULL, TWO, 1),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(NULL, FOUR, 3),
                Triple.of(NULL, THREE_AND_FIVE, 4),
                Triple.of(NULL, SIX_AND_THREETWOFOUR, 5));

        testFindChildLinks(null, null, expectedResults);
    }

    @Test
    public void testFindChildLinksOfTaskWithNestedChildren() {
        String parent = TWOTWO;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2));

        testFindChildLinks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildLinksWithNameFilterOfTaskWithChildren() {
        String parent = TWO;

        TaskFilter filter = createFilter(ONE, null, null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(parent, TWOONE, 0));

        testFindChildLinks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildLinksWithIncludedTagFilterOfTaskWithChildren() {
        String parent = TWOTWO;

        TaskFilter filter = createFilter(null, Set.of(REPEATONCE), null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(parent, TWOTWOTHREE_AND_THREETWOTWO, 2));

        testFindChildLinks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildLinksWithMultipleFilterOfTaskWithChildren() {
        String parent = TWOTWO;

        TaskFilter filter = createFilter("T", Set.of(REPEATONCE), null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(parent, TWOTWOTHREE_AND_THREETWOTWO, 2));

        testFindChildLinks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildLinksOfTaskWithoutChildren() {
        String parent = FOUR;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of();

        testFindChildLinks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildLinksWithFilterOfTaskWithoutChildren() {
        String parent = FOUR;

        TaskFilter filter = createFilter(ONE, null, null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of();

        testFindChildLinks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildLinksWithFilterOfTaskWithChildrenAllFilteredOut() {
        String parent = TWO;

        TaskFilter filter = createFilter("Nonexistent", null, null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of();

        testFindChildLinks(parent, filter, expectedResults);
    }
    
    // Testing findChildTasks

    private void testFindChildTasks(String parent, TaskFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findChildTasks(
                getTaskID(parent), filter), true);
    }

    @Test
    public void testFindChildTasksOfTaskWithChildren() {
        String parent = TWO;

        Iterable<String> expectedResults = List.of(
                TWOONE,
                TWOTWO,
                TWOTHREE);

        testFindChildTasks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildTasksOfRoot() {
        Iterable<String> expectedResults = List.of(
                ONE,
                TWO,
                THREE_AND_FIVE,
                FOUR,
                THREE_AND_FIVE,
                SIX_AND_THREETWOFOUR);

        testFindChildTasks(null, null, expectedResults);
    }

    @Test
    public void testFindChildTasksOfTaskWithNestedChildren() {
        String parent = TWOTWO;

        Iterable<String> expectedResults = List.of(
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO);

        testFindChildTasks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildTasksWithNameFilterOfTaskWithChildren() {
        String parent = TWO;

        TaskFilter filter = createFilter(ONE, null, null);

        Iterable<String> expectedResults = List.of(
                TWOONE);

        testFindChildTasks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildTasksOfTaskWithoutChildren() {
        String parent = FOUR;

        Iterable<String> expectedResults = List.of();

        testFindChildTasks(parent, null, expectedResults);
    }

    @Test
    public void testFindChildTasksWithFilterOfTaskWithoutChildren() {
        String parent = FOUR;

        TaskFilter filter = createFilter(ONE, null, null);

        Iterable<String> expectedResults = List.of();

        testFindChildTasks(parent, filter, expectedResults);
    }

    @Test
    public void testFindChildTasksWithFilterOfTaskWithChildrenAllFilteredOut() {
        String parent = TWO;

        TaskFilter filter = createFilter("Nonexistent", null, null);

        Iterable<String> expectedResults = List.of();

        testFindChildTasks(parent, filter, expectedResults);
    }

    // Testing findParentLinks

    private void testFindParentLinks(String child, TaskFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findParentLinks(getTaskID(child),
                filter), false);
    }

    @Test
    public void testFindParentLinksOfTaskWithParents() {
        String child = TWOTWO;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWO, TWOTWO, 1));

        testFindParentLinks(child, null, expectedResults);
    }

    @Test
    public void testFindParentLinksOfTaskWithMultipleParents() {
        String child = TWOTWOTHREE_AND_THREETWOTWO;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1)
        );

        testFindParentLinks(child, null, expectedResults);
    }

    @Test
    public void testFindParentLinksWithFilterOfTaskWithParents() {
        String child = TWOTWOTHREE_AND_THREETWOTWO;

        TaskFilter filter = createFilter(TWO, null, null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1)
        );

        testFindParentLinks(child, filter, expectedResults);
    }

    @Test
    public void testFindParentLinksOfTaskWithoutParents() {
        String child = ONE;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(NULL, ONE, 0));

        testFindParentLinks(child, null, expectedResults);
    }

    @Test
    public void testFindParentLinksWithFilterOfTaskWithoutParents() {
        String child = ONE;

        TaskFilter filter = createFilter(TWO, null, null);

        Iterable<Triple<String, String, Integer>> expectedResults = List.of();

        testFindParentLinks(child, filter, expectedResults);
    }

    // Testing findParentTasks

    private void testFindParentTasks(String child, TaskFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findParentTasks(
                getTaskID(child), filter), false);
    }

    @Test
    public void testFindParentTasksOfTaskWithParents() {
        String child = TWOTWO;

        Iterable<String> expectedResults = List.of(
                TWO);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksOfTaskWithMultipleParents() {
        String child = TWOTWOTHREE_AND_THREETWOTWO;

        Iterable<String> expectedResults = List.of(
                TWOTWO,
                THREETWO);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksOfTaskWithSameParentMultipleTimes() {
        String child = THREETWOONE_AND_THREETWOTHREE;

        Iterable<String> expectedResults = List.of(
                THREETWO,
                THREETWO);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksWithFilterOfTaskWithParents() {
        String child = TWOTWOTHREE_AND_THREETWOTWO;

        TaskFilter filter = createFilter(TWO, null, null);

        Iterable<String> expectedResults = List.of(
                TWOTWO,
                THREETWO);

        testFindParentTasks(child, filter, expectedResults);
    }

    @Test
    public void testFindParentTasksOfBaseTask() {
        String child = ONE;

        Iterable<String> expectedResults = List.of(
                NULL);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksOfBaseTaskInBaseMultipleTimes() {
        String child = THREE_AND_FIVE;

        Iterable<String> expectedResults = List.of(
                NULL,
                NULL);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksOfBaseTaskWithOtherParents() {
        String child = SIX_AND_THREETWOFOUR;

        Iterable<String> expectedResults = List.of(
                NULL,
                THREETWO);

        testFindParentTasks(child, null, expectedResults);
    }

    @Test
    public void testFindParentTasksWithFilterOfTaskWithoutParents() {
        String child = ONE;

        TaskFilter filter = createFilter(TWO, null, null);

        Iterable<String> expectedResults = List.of();

        testFindParentTasks(child, filter, expectedResults);
    }

    @Test
    public void testFindParentTasksWithFilterOfTaskWithParentsAllFilteredOut() {
        String child = TWOTWOTHREE_AND_THREETWOTWO;

        TaskFilter filter = createFilter("Nonexistent", null, null);

        Iterable<String> expectedResults = List.of();

        testFindParentTasks(child, filter, expectedResults);
    }

    // Testing getAncestorLinks

    private void testFindAncestorLinks(String descendant, TaskFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findAncestorLinks(getTaskID(descendant),
                filter), false);
    }

    @Test
    public void testFindAncestorLinksOfDescendantWithoutAncestors() {
        String descendant = ONE;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(NULL, ONE, 0));

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorLinksOfDescendantWithOneLevelOfAncestors() {
        String descendant = TWOTWO;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(NULL, TWO, 1));

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorLinksOfDescendantWithMultipleLevelsOfAncestors() {
        String descendant = TWOTWOONE;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(NULL, TWO, 1));

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    // Testing getAncestorTasks

    private void testFindAncestorTasks(String descendant, TaskFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findAncestorTasks(
                getTaskID(descendant), filter), false);
    }

    @Test
    public void testFindAncestorTasksOfDescendantWithoutAncestors() {
        String descendant = ONE;

        Iterable<String> expectedResults = List.of(
                NULL);

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorTasksOfDescendantWithOneLevelOfAncestors() {
        String descendant = TWOTWO;

        Iterable<String> expectedResults = List.of(
                TWO,
                NULL);

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorTasksOfDescendantWithMultipleLevelsOfAncestors() {
        String descendant = TWOTWOONE;

        Iterable<String> expectedResults = List.of(
                TWOTWO,
                TWO,
                NULL);

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorLinksOfDescendantNestedMultipleTimesSameLevel() {
        String descendant = THREETWOONE_AND_THREETWOTHREE;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 4),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 4)
        );

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorLinksOfDescendantNestedMultipleTimesDifferentLevels() {
        String descendant = SIX_AND_THREETWOFOUR;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 4),
                Triple.of(NULL, SIX_AND_THREETWOFOUR, 5)
        );

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorLinksOfDescendantAsBaseTask() {
        String descendant = THREE_AND_FIVE;

        Iterable<Triple<String, String, Integer>> expectedResults = List.of(
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 5));

        testFindAncestorLinks(descendant, null, expectedResults);
    }

    // Testing getAncestorTasks

    @Test
    public void testFindAncestorTasksOfDescendantNestedMultipleTimesSameLevel() {
        String descendant = THREETWOONE_AND_THREETWOTHREE;

        Iterable<String> expectedResults = List.of(
                THREETWO,
                THREE_AND_FIVE,
                NULL,
                NULL,
                THREETWO,
                THREE_AND_FIVE,
                NULL,
                NULL
        );

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorTasksOfDescendantNestedMultipleTimesDifferentLevels() {
        String descendant = SIX_AND_THREETWOFOUR;

        Iterable<String> expectedResults = List.of(
                THREETWO,
                THREE_AND_FIVE,
                NULL,
                NULL,
                NULL
        );

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    @Test
    public void testFindAncestorTasksOfDescendantAsBaseTask() {
        String descendant = THREE_AND_FIVE;

        Iterable<String> expectedResults = List.of(
                NULL,
                NULL
        );

        testFindAncestorTasks(descendant, null, expectedResults);
    }

    // Ancestor tests, with filters

    @Test
    public void testFindAncestorsFiltered() {
        String descendant = TWOTWOTWO;

        TaskFilter filter = createFilter(TWO, null, null);

        Iterable<String> expectedTasks = List.of(
                TWOTWO,
                TWO
        );

        Iterable<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWO, TWOTWO, 1)
        );

        testFindAncestorLinks(descendant, filter, expectedLinks);
        testFindAncestorTasks(descendant, filter, expectedTasks);
    }

    @Test
    public void testFindAncestorsFilteredNestedMultipleTimes() {
        String descendant = THREETWOONE_AND_THREETWOTHREE;

        TaskFilter filter = createFilter(THREETWO, null, null);

        Iterable<String> expectedTasks = List.of(
                THREETWO,
                THREETWO
        );

        Iterable<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2)
        );

        testFindAncestorLinks(descendant, filter, expectedLinks);
        testFindAncestorTasks(descendant, filter, expectedTasks);
    }

    @Test
    public void testFindAncestorsFilteredNestedMultipleLevels() {
        String descendant = SIX_AND_THREETWOFOUR;

        TaskFilter filter = createFilter(THREETWO, null, null);

        Iterable<String> expectedTasks = List.of(
                THREETWO
        );

        Iterable<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3)
        );

        testFindAncestorLinks(descendant, filter, expectedLinks);
        testFindAncestorTasks(descendant, filter, expectedTasks);
    }

    @Test
    public void testFindAncestorsFilteredByTag() {
        String descendant = SIX_AND_THREETWOFOUR;

        TaskFilter filter = createFilter(null, Set.of(REPEATSEVERAL), null);

        Iterable<String> expectedTasks = List.of(
                THREETWO
        );

        Iterable<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3)
        );

        testFindAncestorLinks(descendant, filter, expectedLinks);
        testFindAncestorTasks(descendant, filter, expectedTasks);
    }

    @Test
    public void testFindAncestorsFilteredByTag2() {
        String descendant = THREETWOONE_AND_THREETWOTHREE;

        TaskFilter filter = createFilter(null, Set.of(REPEATSEVERAL), Set.of(REPEATONCE));

        Iterable<String> expectedTasks = List.of(
                THREETWO,
                THREETWO
        );

        Iterable<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2)
        );

        testFindAncestorLinks(descendant, filter, expectedLinks);
        testFindAncestorTasks(descendant, filter, expectedTasks);
    }

    // Testing findDescendantLinks

    protected void testFindDescendantLinks(String ancestor, TaskFilter filter, Iterable<Triple<String, String, Integer>> expected) {
        assertLinkQueryResults(expected, entityQueryService.findDescendantLinks(
                getTaskID(ancestor), filter), true);
    }

    // Testing findDescendantTasks

    protected void testFindDescendantTasks(String ancestor, TaskFilter filter, Iterable<String> expected) {
        assertTaskQueryResults(expected, entityQueryService.findDescendantTasks(
                getTaskID(ancestor), filter), true);
    }

    @Test
    public void testFindDescendantsOfAncestorWithoutDescendants() {
        String ancestor = FOUR;

        Iterable<String> expectedResults = List.of();

        testFindDescendantTasks(ancestor, null, expectedResults);
    }

    @Test
    public void testFindDescendantsOfAncestorWithOneLevelOfDescendants() {
        String ancestor = TWOTWO;

        Collection<String> expectedResults = List.of(
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2)
        );

        testFindDescendantLinks(ancestor, null, expectedLinks);
        testFindDescendantTasks(ancestor, null, expectedResults);
    }

    @Test
    public void testFindDescendantsOfAncestorWithMultipleLevelsOfDescendants() {
        String ancestor = TWO;

        Collection<String> expectedResults = List.of(
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO,
                TWOTHREE
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(TWO, TWOONE, 0),
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2),
                Triple.of(TWO, TWOTHREE, 2)
        );

        testFindDescendantLinks(ancestor, null, expectedLinks);
        testFindDescendantTasks(ancestor, null, expectedResults);
    }

    @Test
    public void testFindDescendantsOfAncestorComplexNesting() {
        String ancestor = THREE_AND_FIVE;

        Collection<String> expectedTasks = List.of(
                THREEONE,
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                TWOTWOTHREE_AND_THREETWOTWO,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR,
                THREETHREE
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREE_AND_FIVE, THREEONE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETHREE, 2)
        );

        testFindDescendantLinks(ancestor, null, expectedLinks);
        testFindDescendantTasks(ancestor, null, expectedTasks);
    }

    @Test
    public void testFindDescendantsFiltered() {
        String ancestor = TWO;

        TaskFilter filter = createFilter(TWOTWO, null, null);

        Collection<String> expectedTasks = List.of(
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO);

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2)
        );

        testFindDescendantLinks(ancestor, filter, expectedLinks);
        testFindDescendantTasks(ancestor, filter, expectedTasks);
    }

    // TODO: Test tag filter with outer join

    @Test
    public void testFindDescendantsFilteredComplexNesting() {
        String ancestor = THREE_AND_FIVE;

        TaskFilter filter = createFilter(THREETWO, Set.of(REPEATSEVERAL), Set.of(REPEATONCE));

        Collection<String> expectedTasks = List.of(
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3)
        );

        testFindDescendantLinks(ancestor, filter, expectedLinks);
        testFindDescendantTasks(ancestor, filter, expectedTasks);
    }

    @Test
    public void testFindDescendantsFilteredComplexNesting2() {
        String ancestor = TWO;

        TaskFilter filter = createFilter(null, null, Set.of(REPEATONCE, REPEATSEVERAL));

        Collection<String> expectedTasks = List.of(
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTHREE
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(TWO, TWOONE, 0),
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWO, TWOTHREE, 2)
        );

        testFindDescendantLinks(ancestor, filter, expectedLinks);
        testFindDescendantTasks(ancestor, filter, expectedTasks);
    }

    @Test
    void testFindFullTree() {
        Collection<String> expectedTasks = List.of(
                ONE,
                TWO,
                TWOONE,
                TWOTWO,
                TWOTWOONE,
                TWOTWOTWO,
                TWOTWOTHREE_AND_THREETWOTWO,
                TWOTHREE,
                THREE_AND_FIVE,
                THREEONE,
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                TWOTWOTHREE_AND_THREETWOTWO,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR,
                THREETHREE,
                FOUR,
                THREE_AND_FIVE,
                THREEONE,
                THREETWO,
                THREETWOONE_AND_THREETWOTHREE,
                TWOTWOTHREE_AND_THREETWOTWO,
                THREETWOONE_AND_THREETWOTHREE,
                SIX_AND_THREETWOFOUR,
                THREETHREE,
                SIX_AND_THREETWOFOUR
        );

        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
                Triple.of(NULL, ONE, 0),
                Triple.of(NULL, TWO, 1),
                Triple.of(TWO, TWOONE, 0),
                Triple.of(TWO, TWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOONE, 0),
                Triple.of(TWOTWO, TWOTWOTWO, 1),
                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 2),
                Triple.of(TWO, TWOTHREE, 2),
                Triple.of(NULL, THREE_AND_FIVE, 2),
                Triple.of(THREE_AND_FIVE, THREEONE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETHREE, 2),
                Triple.of(NULL, FOUR, 3),
                Triple.of(NULL, THREE_AND_FIVE, 4),
                Triple.of(THREE_AND_FIVE, THREEONE, 0),
                Triple.of(THREE_AND_FIVE, THREETWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 0),
                Triple.of(THREETWO, TWOTWOTHREE_AND_THREETWOTWO, 1),
                Triple.of(THREETWO, THREETWOONE_AND_THREETWOTHREE, 2),
                Triple.of(THREETWO, SIX_AND_THREETWOFOUR, 3),
                Triple.of(THREE_AND_FIVE, THREETHREE, 2),
                Triple.of(NULL, SIX_AND_THREETWOFOUR, 5)
        );

        testFindDescendantLinks(null, null, expectedLinks);
        testFindDescendantTasks(null, null, expectedTasks);
    }

    // TODO: Priority
//    @Test
//    public void testFindDescendantsFilteredByPriority() {
//        String ancestor = TWO;
//
//        TaskFilter filter = createFilter(null, null, Priority.HIGH);
//
//        Collection<String> expectedTasks = List.of(
//                TWOTWO,
//                TWOTWOONE,
//                TWOTWOTWO,
//                TWOTWOTHREE_AND_THREETWOTWO
//        );
//
//        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
//                Triple.of(TWO, TWOTWO, 1),
//                Triple.of(TWOTWO, TWOTWOONE, 1),
//                Triple.of(TWOTWO, TWOTWOTWO, 1),
//                Triple.of(TWOTWO, TWOTWOTHREE_AND_THREETWOTWO, 1)
//        );
//
//        testFindDescendantLinks(ancestor, filter, expectedLinks);
//        testFindDescendantTasks(ancestor, filter, expectedTasks);
//    }

//    @Test
//    public void testFindDescendantsFilteredByTagAndPriority() {
//        String ancestor = THREEANDFIVE;
//
//        TaskFilter filter = createFilter(null, Set.of(REPEATSEVERAL), Priority.HIGH);
//
//        Collection<String> expectedTasks = List.of(
//                THREETWO,
//                TWOTWO
//        );
//
//        Collection<Triple<String, String, Integer>> expectedLinks = List.of(
//                Triple.of(THREEANDFIVE, THREETWO, 1),
//                Triple.of(THREETWO, TWOTWO, 1)
//        );
//
//        testFindDescendantLinks(ancestor, filter, expectedLinks);
//        testFindDescendantTasks(ancestor, filter, expectedTasks);
//    }
}
