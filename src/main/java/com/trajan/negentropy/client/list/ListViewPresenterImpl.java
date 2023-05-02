//package com.trajan.negentropy.client.list;
//
//import com.trajan.negentropy.client.TaskEntry;
//import com.trajan.negentropy.client.list.util.Insert;
//import com.trajan.negentropy.client.list.util.TaskEntryDataProvider;
//import com.trajan.negentropy.server.entity.Tag;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.TaskLink;
//import com.trajan.negentropy.server.service.TagService;
//import exclude.TaskService;
//import com.vaadin.flow.spring.annotation.SpringComponent;
//import com.vaadin.flow.spring.annotation.VaadinSessionScope;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@SpringComponent
//@VaadinSessionScope
//public class ListViewPresenterImpl implements ListViewPresenter {
//    private static final Logger logger = LoggerFactory.getLogger(ListViewPresenterImpl.class);
//
//    TaskService taskService;
//    TagService tagService;
//
//    private ListView listView;
//    private TaskForm taskForm;
//
//    private TaskEntry rootEntry = null;
//
//    public ListViewPresenterImpl(TaskService taskService, TagService tagService) {
//        this.taskService = taskService;
//        this.tagService = tagService;
//    }
//
//    @Override
//    public void initListView(ListView listView) {
//        this.listView = listView;
//        loadTaskNodes();
//    }
//
//    @Transactional
//    private void loadTaskNodes() {
//        logger.debug("Refreshed task nodes.");
//        TaskEntryDataProvider dataProvider = rootEntry != null ?
//                new TaskEntryDataProvider(taskService, rootEntry.link().getReferenceTask()) :
//                new TaskEntryDataProvider(taskService, null);
//        listView.getTreeGrid().setDataProvider(dataProvider);
//    }
//
//    @Override
//    public void setRootEntry(TaskEntry entry) {
//        this.rootEntry = entry;
//        loadTaskNodes();
//    }
//
//    @Override
//    public TaskEntry getRootEntry() {
//        return this.rootEntry;
//    }
//
//    @Override
//    public void initTaskInfoForm(TaskForm taskForm) {
//        this.taskForm = taskForm;
//    }
//
//    @Override
//    @Transactional
//    public void moveNodeInto(TaskLink moved, TaskLink target) {
//        taskService.createChildNode(
//                target.getReferenceTask().getId(),
//                moved.getReferenceTask().getId(),
//                0);
//        taskService.deleteNode(moved.getId());
//        loadTaskNodes();
//    }
//
//    @Override
//    public void moveNodeAsOrphan(TaskLink link) {
//        taskService.createOrphanNode(link.getReferenceTask().getId());
//        taskService.deleteNode(link.getId());
//        loadTaskNodes();
//    }
//
//    @Override
//    @Transactional
//    public void moveNodeBefore(TaskLink moved, TaskLink target) {
//        taskService.createNodeBefore(
//                moved.getReferenceTask().getId(),
//                target.getId(),
//                0);
//        taskService.deleteNode(moved.getId());
//        loadTaskNodes();
//    }
//
//    @Override
//    @Transactional
//    public void moveNodeAfter(TaskLink moved, TaskLink target) {
//        taskService.createNodeAfter(
//                moved.getReferenceTask().getId(),
//                target.getId(),
//                0);
//        taskService.deleteNode(moved.getId());
//        loadTaskNodes();
//    }
//
//    @Override
//    @Transactional
//    public void insertTaskNodeFromForm(Insert position, TaskLink link) {
//        if (isValid()) {
//            Task task = taskForm.getBinder().getBean();
//            if (task.getId() == null) task = taskService.createTaskWithNode(task).getFirst();
//            switch (position) {
//                // TODO: Priority
//                case AS_SUBTASK_OF -> taskService.createChildNode(
//                        link.getReferenceTask().getId(),
//                        task.getId(),
//                        0);
//                case BEFORE -> taskService.createNodeBefore(
//                        task.getId(),
//                        link.getId(),
//                        0);
//                case AFTER -> taskService.createNodeAfter(
//                        task.getId(),
//                        link.getId(),
//                        0);
//                default -> throw new IllegalArgumentException(
//                        "Unknown insertion position: " + position);
//            }
//            loadTaskNodes();
//        }
//    }
//
//    @Override
//    public boolean isValid() {
//        return taskForm.getBinder().isValid();
//    }
//
//    @Override
//    @Transactional
//    public void deleteNode(TaskLink link) {
//        taskService.deleteNode(link.getId());
//        clearSelectedTask();
//        loadTaskNodes();
//    }
//
//    @Override
//    @Transactional
//    public Task saveTaskFromForm() {
//        Task task = null;
//        if (isValid()) {
//            task = taskForm.getBinder().getBean();
//            if (task.getId() == null) {
//                if (rootEntry != null) {
//                    task = taskService.createTask(task);
//                    taskService.createChildNode(
//                            rootEntry.link().getReferenceTask().getId(),
//                            task.getId(),
//                            0);
//                } else {
//                    task = taskService.createTaskWithNode(task).getFirst();
//                }
//            }
//            else task = taskService.updateTask(task);
//            taskForm.getBinder().setBean(task);
//            loadTaskNodes();
//        }
//        return task;
//    }
//
//    @Override
//    @Transactional
//    public void deleteTask(Task task) {
//        if (task.getId() != null){
//            taskService.deleteTask(task.id());
//            loadTaskNodes();
//        }
//    }
//
//    @Override
//    public void setTaskBean(Task data) {
//        taskForm.getBinder().setBean(data);
//    }
//
//    @Override
//    public void clearSelectedTask() {
//        taskForm.getBinder().setBean(new Task());
//        listView.getTreeGrid().deselectAll();
//    }
//
//    @Override
//    public TaskService getTaskService() {
//        return taskService;
//    }
//
//    @Override
//    public List<Tag> findAllTags() {
//        return tagService.findAll();
//    }
//
//    @Override
//    public Tag createTag(Tag tag) {
//        taskForm.tagBox.setItems(tagService.findAll());
//        return tagService.create(tag);
//    }
//}