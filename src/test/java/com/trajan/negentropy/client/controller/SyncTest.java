package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.sync.SyncRecord;
import com.trajan.negentropy.model.id.ID.SyncID;
import com.trajan.negentropy.model.id.LinkID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.DeleteChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SyncTest extends ClientTestTemplate {

    @BeforeAll
    protected void beforeAll() {
        init();
    }

    private SyncRecord assertMoveChanges(TaskNode input, TaskNode reference, InsertLocation location) {
        DataMapResponse response = controller.requestChange(Change.move(
                input.id(),
                reference.id(),
                location));

        System.out.println("Assert move change: node " + input.id() + " with " + input.task() + " to " + location + " " + reference.task());
        assertTrue(response.success());
        assertMoveChangesPresent(response.aggregateSyncRecord(), input);

        return response.aggregateSyncRecord();
    }

    private void assertMoveChangesPresent(SyncRecord syncRecord, TaskNode input) {
        List<Change> changes = syncRecord.changes();
        assertTrue(changes.size() > 1);
        assertTrue(changes.stream().anyMatch(change -> {
            if (change instanceof DeleteChange<?> deleteChange) {
                System.out.println("Delete change: " + deleteChange.data());
                if (deleteChange.data() instanceof LinkID linkID) {
                    return linkID.equals(input.id());
                }
            }
            return false;
        }));
        assertTrue(changes.stream().anyMatch(change -> {
            if (change instanceof PersistChange<?> persistChange) {
                System.out.println("Persist change: " + persistChange.data());
                if (persistChange.data() instanceof TaskNode node) {
                    return node.task().id().equals(input.task().id());
                }
            }
            return false;
        }));
    }

    @Test
    void testMoveSync() {
        SyncID originalSyncId = queryService.currentSyncId();
        assertNotNull(originalSyncId);

        TaskNode nodeOne = nodes.get(Triple.of(NULL, ONE, 0));
        TaskNode nodeTwo = nodes.get(Triple.of(NULL, TWO, 1));

        SyncRecord firstSyncRecord = assertMoveChanges(nodeOne, nodeTwo, InsertLocation.AFTER);
        assertNotEquals(originalSyncId, firstSyncRecord.id());

        TaskNode nodeFour = nodes.get(Triple.of(NULL, FOUR, 3));
        TaskNode nodeSix = nodes.get(Triple.of(NULL, SIX_AND_THREETWOFOUR, 5));

        controller.taskNetworkGraph.syncId(originalSyncId);

        SyncRecord secondSyncRecord = assertMoveChanges(nodeFour, nodeSix, InsertLocation.AFTER);
        assertNotEquals(originalSyncId, secondSyncRecord.id());
        assertMoveChangesPresent(secondSyncRecord, nodeOne);
    }
}
