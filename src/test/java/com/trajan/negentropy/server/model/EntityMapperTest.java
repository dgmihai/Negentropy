//package com.trajan.negentropy.server.model;
//
//import com.trajan.negentropy.server.backend.TaskEntityQueryService;
//import com.trajan.negentropy.server.backend.entity.TaskEntity;
//import com.trajan.negentropy.server.backend.entity.TaskLink;
//import com.trajan.negentropy.server.facade.model.EntityMapper;
//import com.trajan.negentropy.server.facade.model.Task;
//import com.trajan.negentropy.server.facade.model.TaskNode;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.Duration;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.when;
//
//@ExtendWith(MockitoExtension.class)
//public class EntityMapperTest {
//
//    @InjectMocks private EntityMapper entityMapper;
//
////    @Mock private DataContext dataContext;
//    @Mock private TaskEntityQueryService entityQueryService;
//
//    // Test data
//    private TaskEntity taskEntity;
//    private Task task;
//
//    private List<TaskLink> linkEntities = new ArrayList<>();
//    private List<TaskLink> links = new ArrayList<>();
//
//    @BeforeEach
//    public void setUp() {
//        // TaskEntity instances
//        taskEntity = TaskEntity.builder()
//                .id(3L)
//                .name("Task 1")
//                .description("Task 1 Description")
//                .duration(Duration.ofHours(1))
//                .build();
//
//        // Related task entities
//        TaskEntity parent = TaskEntity.builder()
//                .id(10L)
//                .name("Parent")
//                .build();
//        TaskEntity child1 = TaskEntity.builder()
//                .id(11L)
//                .name("Child 1")
//                .build();
//        TaskEntity child2 = TaskEntity.builder()
//                .id(12L)
//                .name("Child 2")
//                .build();
//        TaskEntity child3 = TaskEntity.builder()
//                .id(13L)
//                .name("Child 3")
//                .build();
//
//        // TaskLink instances
//        TaskLink linkEntityP = TaskLink.builder()
//                .id(20L)
//                .child(taskEntity)
//                .parent(parent)
//                .build();
//        TaskLink linkEntity1 = TaskLink.builder()
//                .id(21L)
//                .child(child1)
//                .parent(taskEntity)
//                .build();
//        TaskLink linkEntity2 = TaskLink.builder()
//                .id(22L)
//                .child(child2)
//                .parent(taskEntity)
//                .build();
//        TaskLink linkEntity3 = TaskLink.builder()
//                .id(23L)
//                .child(child3)
//                .parent(taskEntity)
//                .build();
//
//        // TaskLink instances
//        TaskNode linkP = new TaskLink(
//                linkEntityP.id(),
//                linkEntityP.priority(),
//                linkEntityP.position(),
//                linkEntityP.parent().id(),
//                linkEntityP.child());
//        TaskNode link1 = new TaskLink(
//                linkEntity1.id(),
//                linkEntity1.priority(),
//                linkEntity1.position(),
//                linkEntity1.parent().id(),
//                linkEntity1.child().id());
//        TaskNode link2 = new TaskLink(
//                linkEntity2.id(),
//                linkEntity2.priority(),
//                linkEntity2.position(),
//                linkEntity2.parent().id(),
//                linkEntity2.child().id());
//        TaskNode link3 = new TaskLink(
//                linkEntity3.id(),
//                linkEntity3.priority(),
//                linkEntity3.position(),
//                linkEntity3.parent().id(),
//                linkEntity3.child().id());
//
//        List<TaskLink> childLinkEntities = List.of(linkEntity1, linkEntity2, linkEntity3);
//        List<TaskLink> childLinks = List.of(link1, link2, link3);
//
//        taskEntity.childLinks(childLinkEntities).parentLinks(List.of(linkEntityP));
//
//        // Task instances
//        task = Task.builder()
//                .id(3L)
//                .name("Task 1")
//                .description("Task 1 Description")
//                .duration(Duration.ofHours(1))
////                .childLinks(childLinks)
////                .parentLinks(List.of(linkP))
//                .build();
//
//        linkEntities.add(linkEntityP);
//        linkEntities.addAll(childLinkEntities);
//
////        links.add(linkP);
//        links.addAll(childLinks);
//    }
//
//    @Test
//    public void toDTO_taskEntity_returnsTask() {
//        Task result = EntityMapper.toDTO(taskEntity);
//
//        assertEquals(task, result);
//    }
//
//    @Test
//    public void toEntity_task_returnsTaskEntity() {
//        when(entityQueryService.getTask(task.id())).thenReturn(taskEntity);
////        when(dataContext.updateTask(taskEntity)).thenReturn(taskEntity);
//
//        TaskEntity result = entityMapper.merge(task);
//
//        assertEquals(taskEntity, result);
////        verify(dataContext).updateTask(taskEntity);
//    }
//
//    @Test
//    public void toEntity_existingTask_updatesExistingTaskEntity() {
//        Task updatedTask = task.toBuilder()
//                .name("Updated Task 1")
//                .description("Updated Task 1 Description")
//                .duration(Duration.ofHours(2))
////                .childLinks(links.subList(1, 4))
////                .parentLinks(links.subList(0, 1))
//                .build();
//
//        TaskEntity expectedUpdatedTaskEntity = taskEntity.toBuilder()
//                .name("Updated Task 1")
//                .description("Updated Task 1 Description")
//                .duration(Duration.ofHours(2))
//                .childLinks(linkEntities.subList(1, 4))
//                .parentLinks(linkEntities.subList(0, 1))
//                .build();
//
//        when(entityQueryService.getTask(updatedTask.id())).thenReturn(taskEntity);
////        when(dataContext.updateTask(expectedUpdatedTaskEntity)).thenReturn(expectedUpdatedTaskEntity);
//
//        TaskEntity result = entityMapper.merge(updatedTask);
//
//        assertEquals(expectedUpdatedTaskEntity, result);
////        verify(dataContext).updateTask(expectedUpdatedTaskEntity);
//    }
//
//    @Test
//    public void toDTO_taskEntityWithMultipleLinks_returnsTaskWithOrderedLinks() {
//        Task result = EntityMapper.toDTO(taskEntity);
//
//        assertEquals(task, result);
////        assertEquals(task.childLinks().size(), result.childLinks().size());
////        for (int i = 0; i < task.childLinks().size(); i++) {
////            assertEquals(task.childLinks().get(i), result.childLinks().get(i));
////        }
//    }
//
//    @Test
//    public void toEntity_taskWithMultipleLinks_returnsTaskEntityWithOrderedLinks() {
//        when(entityQueryService.getTask(task.id())).thenReturn(taskEntity);
////        when(dataContext.updateTask(taskEntity)).thenReturn(taskEntity);
//
//        TaskEntity result = entityMapper.merge(task);
//
//        assertEquals(taskEntity, result);
//        assertEquals(taskEntity.childLinks().size(), result.childLinks().size());
//        for (int i = 0; i < taskEntity.childLinks().size(); i++) {
//            assertEquals(taskEntity.childLinks().get(i), result.childLinks().get(i));
//        }
//    }
//}