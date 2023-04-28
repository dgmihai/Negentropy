//package com.trajan.negentropy.client.list;
//
//import com.trajan.negentropy.client.TaskEntry;
//import com.trajan.negentropy.client.list.util.Insert;
//import com.trajan.negentropy.server.entity.TagEntity;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.TaskLink;
//import exclude.TaskService;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import com.vaadin.flow.spring.annotation.VaadinSessionScope;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@SpringComponent
//@VaadinSessionScope
//public interface ListViewPresenter {
//    void initListView(ListView listView);
//
//    void setRootEntry(TaskEntry entry);
//
//    TaskEntry getRootEntry();
//
//    void initTaskInfoForm(TaskForm taskForm);
//
//    @Transactional
//    void moveNodeInto(TaskLink moved, TaskLink target);
//
//    @Transactional
//    void moveNodeAsOrphan(TaskLink link);
//
//    @Transactional
//    void moveNodeBefore(TaskLink moved, TaskLink target);
//
//    @Transactional
//    void moveNodeAfter(TaskLink moved, TaskLink target);
//
//    @Transactional
//    void insertTaskNodeFromForm(Insert position, TaskLink link);
//
//    boolean isValid();
//
//    @Transactional
//    void deleteNode(TaskLink link);
//
//    @Transactional
//    Task saveTaskFromForm();
//
//    @Transactional
//    void deleteTask(Task task);
//
//    void setTaskBean(Task data);
//
//    void clearSelectedTask();
//
//    TaskService getTaskService();
//
//    List<TagEntity> findAllTags();
//
//    TagEntity createTag(TagEntity tag);
//}