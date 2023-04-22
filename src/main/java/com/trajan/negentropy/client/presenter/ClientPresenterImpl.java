package com.trajan.negentropy.client.presenter;

import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.service.TaskService;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@SpringComponent
@VaadinSessionScope
public class ClientPresenterImpl implements ClientPresenter {
    private static final Logger logger = LoggerFactory.getLogger(ClientPresenterImpl.class);

    private final TaskService taskService;

    private ListView listView;
    private TaskInfoForm taskInfoForm;

    private TaskEntry rootEntry = null;

    public ClientPresenterImpl(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void initListView(ListView listView) {
        this.listView = listView;
        loadTaskNodes();
    }

    @Transactional
    private void loadTaskNodes() {
        logger.debug("Refreshed task nodes.");
        TaskEntryDataProvider dataProvider = rootEntry != null ?
                new TaskEntryDataProvider(taskService, rootEntry.node().getReferenceTask()) :
                new TaskEntryDataProvider(taskService, null);
        listView.getTreeGrid().setDataProvider(dataProvider);
    }

    @Override
    public void setRootEntry(TaskEntry entry) {
        this.rootEntry = entry;
        loadTaskNodes();
    }

    @Override
    public TaskEntry getRootEntry() {
        return this.rootEntry;
    }

    @Override
    public void initTaskInfoForm(TaskInfoForm taskInfoForm) {
        this.taskInfoForm = taskInfoForm;
    }

    @Override
    @Transactional
    public void insertTaskNode(Insert position, TaskNode node) {
        if (isValid()) {
            Task task = taskInfoForm.getBinder().getBean();
            if (task.getId() == null) task = taskService.createTaskWithNode(task).getFirst();
            switch (position) {
                // TODO: Priority
                case AS_SUBTASK -> taskService.createChildNode(
                        task.getId(),
                        node.getReferenceTask().getId(),
                        0);
                case BEFORE -> taskService.createNodeBefore(
                        task.getId(),
                        node.getId(),
                        0);
                case AFTER -> taskService.createNodeAfter(
                        task.getId(),
                        node.getId(),
                        0);
                default -> throw new IllegalArgumentException(
                        "Unknown insertion position: " + position);
            }
            loadTaskNodes();
        }
    }

    @Override
    public boolean isValid() {
        return taskInfoForm.getBinder().isValid();
    }

    @Override
    @Transactional
    public void deleteNode(TaskNode node) {
        taskService.deleteNode(node.getId());
        clearSelectedTask();
        loadTaskNodes();
    }

    @Override
    @Transactional
    public Task saveTask() {
        Task task = null;
        if (isValid()) {
            task = taskInfoForm.getBinder().getBean();
            if (task.getId() == null) task = taskService.createTaskWithNode(task).getFirst();
            else task = taskService.updateTask(task);
            taskInfoForm.getBinder().setBean(task);
            loadTaskNodes();
        }
        return task;
    }

    @Override
    @Transactional
    public void deleteTask(Task task) {
        if (task.getId() != null){
            taskService.deleteTask(task.getId());
            loadTaskNodes();
        }
    }

    @Override
    public void setTaskBean(Task data) {
        taskInfoForm.getBinder().setBean(data);
    }

    @Override
    public void clearSelectedTask() {
        taskInfoForm.getBinder().setBean(new Task());
        listView.getTreeGrid().deselectAll();
    }
}