package com.trajan.negentropy.server.backend.util;

import com.google.common.collect.ArrayListMultimap;
import com.trajan.negentropy.model.interfaces.Ancestor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AncestorOrderingUtilTest {
    AncestorOrderingUtil<String, TestEntity> ancestorUtil;

    private static final String ORIGINAL = "original";
    private static final String NEW = "new";

    @BeforeEach
    void setUp() {
        ancestorUtil = new AncestorOrderingUtil<>(TestEntity::id);
    }

    private ArrayListMultimap<String, TestEntity> createTestEntity(List<String> ids, String otherData) {
        ArrayListMultimap<String, TestEntity> result = ArrayListMultimap.create();
        for (String id : ids) {
            if (!id.isBlank()) {
                TestEntity entity = new TestEntity(id);
                entity.otherData(otherData);
                result.put(id, entity);
            } else {
                TestEntity entity = new TestEntity(null);
                entity.otherData(null);
                result.put(null, entity);
            }
        }
        return result;
    }
    
    record Expectation(String id, String data, int childCount) {};
    
    private Expectation expect(String id, String data, int childCount) {
        return new Expectation(id, data, childCount);
    }

    // Helper method to assert the structure of the rearranged entity
    private void assertStructure(TestEntity root, List<Expectation> expectations) {
        System.out.println("Root: " + root.id() + " " + root.otherData() + " " + root.children().size());
        List<TestEntity> entities = DFSUtil.traverse(root);
        assertEquals(expectations.size(), entities.size());

        for (int i=0; i<entities.size(); i++) {
            TestEntity entity = entities.get(i);
            Expectation expectation = expectations.get(i);

            System.out.println("Entity: " + entity.id() + " " + entity.otherData() + " " + entity.children().size());

            assertEquals(expectation.id(), entity.id());
            assertEquals(expectation.data(), entity.otherData());
            assertEquals(expectation.childCount(), entity.children().size());
        }
    }

    @Test
    void testSimpleRearrange() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        a.children().add(b);
        a.children().add(c);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "B", "C"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newC = newEntities.get("C").get(0);
        newA.children().add(newC);
        newA.children().add(newB);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 2),
                expect("C", ORIGINAL, 0),
                expect("B", ORIGINAL, 0)));
    }

    @Test
    void testNestedRearrange() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        a.children().add(b);
        b.children().add(c);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "B", "C"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newC = newEntities.get("C").get(0);
        newB.children().add(newA);
        newA.children().add(newC);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newB);

        assertStructure(rearranged, List.of(
                expect("B", ORIGINAL, 1),
                expect("A", ORIGINAL, 1),
                expect("C", ORIGINAL, 0)));
    }

    @Test
    void testRearrangeWithDuplicates() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        a.children().add(b);
        b.children().add(c);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(
                List.of(
                        "A",
                        "B",
                        "A",
                        "C",
                        "B"),
                NEW);

        TestEntity newA1 = newEntities.get("A").get(0);
        TestEntity newB1 = newEntities.get("B").get(0);
        TestEntity newA2 = newEntities.get("A").get(1);
        TestEntity newC = newEntities.get("C").get(0);
        TestEntity newB2 = newEntities.get("B").get(1);
        newA1.children().add(newB1);
        newB1.children().add(newA2);
        newA2.children().add(newC);
        newC.children().add(newB2);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA1);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("B", ORIGINAL, 1),
                expect("A", NEW, 1),
                expect("C", ORIGINAL, 1),
                expect("B", NEW, 0)));
    }

    @Test
    void testSimpleRearrangeWithNonExistentEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B"),
                ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        a.children().add(b);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "X", "B", "Y"),
                NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newX = newEntities.get("X").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newY = newEntities.get("Y").get(0);
        newA.children().add(newX);
        newA.children().add(newB);
        newA.children().add(newY);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 3),
                expect("X", NEW, 0),
                expect("B", ORIGINAL, 0),
                expect("Y", NEW, 0)));
    }

    @Test
    void testNestedRearrangeWithNonExistentEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        a.children().add(b);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "X", "B", "Y"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newX = newEntities.get("X").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newY = newEntities.get("Y").get(0);
        newA.children().add(newX);
        newX.children().add(newB);
        newB.children().add(newY);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("X", NEW, 1),
                expect("B", ORIGINAL, 1),
                expect("Y", NEW, 0)));
    }

    @Test
    void testMixedRearrangeWithNonExistentEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        a.children().add(b);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "X", "B", "Y"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newX = newEntities.get("X").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newY = newEntities.get("Y").get(0);
        newA.children().add(newX);
        newA.children().add(newB);
        newB.children().add(newY);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 2),
                expect("X", NEW, 0),
                expect("B", ORIGINAL, 1),
                expect("Y", NEW, 0)));
    }

    @Test
    void testSimpleRearrangeWithRemovedEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        a.children().add(b);
        a.children().add(c);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "C"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newC = newEntities.get("C").get(0);
        newA.children().add(newC);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("C", ORIGINAL, 0)));
    }

    @Test
    void testNestedRearrangeWithRemovedEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C", "D"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        TestEntity d = entities.get("D").get(0);
        a.children().add(b);
        b.children().add(c);
        c.children().add(d);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "C"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newC = newEntities.get("C").get(0);
        newA.children().add(newC);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("C", ORIGINAL, 0)));
    }

    @Test
    void testMixedRearrangeWithRemovedAndAddedEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "B", "C", "D"), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        TestEntity d = entities.get("D").get(0);
        a.children().add(b);
        b.children().add(c);
        c.children().add(d);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "B", "X", "Y"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newX = newEntities.get("X").get(0);
        TestEntity newY = newEntities.get("Y").get(0);
        newA.children().add(newB);
        newB.children().add(newX);
        newB.children().add(newY);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("B", ORIGINAL, 2),
                expect("X", NEW, 0),
                expect("Y", NEW, 0)));
    }


    @Test
    void testRearrangeWithExistingNullEntities() {
        ArrayListMultimap<String, TestEntity> entities = createTestEntity(List.of("A", "", "B", "C", "D", ""), ORIGINAL);
        TestEntity a = entities.get("A").get(0);
        TestEntity n1 = entities.get(null).get(0);
        TestEntity b = entities.get("B").get(0);
        TestEntity c = entities.get("C").get(0);
        TestEntity n2 = entities.get(null).get(1);
        TestEntity d = entities.get("D").get(0);
        a.children().add(b);
        a.children().add(n1);
        n1.children().add(c);
        b.children().add(n2);
        b.children().add(d);

        ArrayListMultimap<String, TestEntity> newEntities = createTestEntity(List.of("A", "B", "D"), NEW);
        TestEntity newA = newEntities.get("A").get(0);
        TestEntity newB = newEntities.get("B").get(0);
        TestEntity newD = newEntities.get("D").get(0);
        newA.children().add(newB);
        newB.children().add(newD);

        TestEntity rearranged = ancestorUtil.rearrangeAncestor(a, newA);

        assertStructure(rearranged, List.of(
                expect("A", ORIGINAL, 1),
                expect("B", ORIGINAL, 1),
                expect("D", ORIGINAL, 0)));
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    static class TestEntity implements Ancestor<TestEntity> {
        private final String id;
        private final List<TestEntity> children;
        private String otherData; // Additional fields to test data preservation

        public TestEntity(String id) {
            this.id = id;
            this.children = new ArrayList<>();
        }
    }
}
