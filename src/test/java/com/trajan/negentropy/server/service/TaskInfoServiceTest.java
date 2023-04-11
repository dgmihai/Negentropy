package com.trajan.negentropy.server.service;

import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskRelationship;
import com.trajan.negentropy.server.repository.TaskInfoRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
public class TaskInfoServiceTest {

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private TaskInfoRepository taskInfoRepository;

    @Test
    public void testAddTaskInfoWithTaskRelationship() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle("Test TaskInfo");
        taskInfo.setDescription("This is a test TaskInfo");

        TaskRelationship relationship = new TaskRelationship();
        relationship.setOrderIndex(0);

        taskInfo.addRelationship(relationship);

        taskInfoService.save(taskInfo);

        TaskInfo savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals("Test TaskInfo", savedTaskInfo.getTitle());
        assertEquals("This is a test TaskInfo", savedTaskInfo.getDescription());
        assertEquals(1, savedTaskInfo.getRelationships().size());
        assertNotNull(savedTaskInfo.getRelationships().get(0).getId());
    }

    @Test
    public void testAddTaskInfoWithNoTaskRelationships() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle("Test TaskInfo");
        taskInfo.setDescription("This is a test TaskInfo");

        taskInfoService.save(taskInfo);

        TaskInfo savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals("Test TaskInfo", savedTaskInfo.getTitle());
        assertEquals("This is a test TaskInfo", savedTaskInfo.getDescription());
        assertEquals(1, savedTaskInfo.getRelationships().size());
        assertNotNull(savedTaskInfo.getRelationships().get(0).getId());
        assertNull(savedTaskInfo.getRelationships().get(0).getParentRelationship());
    }

    @Test
    public void testAddTaskRelationshipToExistingTaskInfo() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle("Test TaskInfo");
        taskInfo.setDescription("This is a test TaskInfo");

        taskInfoService.save(taskInfo);

        TaskRelationship relationship = new TaskRelationship();
        relationship.setOrderIndex(0);

        TaskInfo savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals("Test TaskInfo", savedTaskInfo.getTitle());
        assertEquals("This is a test TaskInfo", savedTaskInfo.getDescription());
        assertEquals(1, savedTaskInfo.getRelationships().size());

        savedTaskInfo.addRelationship(relationship);
        taskInfoService.save(savedTaskInfo);

        savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals(2, savedTaskInfo.getRelationships().size());
        assertNotNull(savedTaskInfo.getRelationships().get(0).getId());
    }


    @Test
    public void testDeleteTaskInfoWithTaskRelationships() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle("Test TaskInfo");
        taskInfo.setDescription("This is a test TaskInfo");

        TaskRelationship relationship = new TaskRelationship();
        relationship.setOrderIndex(0);

        taskInfo.addRelationship(relationship);

        taskInfoService.save(taskInfo);

        TaskInfo savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals(1, savedTaskInfo.getRelationships().size());

        taskInfoService.delete(savedTaskInfo);

        savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNull(savedTaskInfo);
    }

    @Test
    public void testDeleteTaskRelationshipFromTaskInfo() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTitle("Test TaskInfo");
        taskInfo.setDescription("This is a test TaskInfo");

        TaskRelationship relationship = new TaskRelationship();
        relationship.setOrderIndex(0);

        taskInfo.addRelationship(relationship);

        taskInfoService.save(taskInfo);

        TaskInfo savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals(1, savedTaskInfo.getRelationships().size());

        savedTaskInfo.removeRelationship(relationship);
        taskInfoService.save(savedTaskInfo);

        savedTaskInfo = taskInfoRepository.findById(taskInfo.getId()).orElse(null);
        assertNotNull(savedTaskInfo);
        assertEquals(0, savedTaskInfo.getRelationships().size());
    }

}
