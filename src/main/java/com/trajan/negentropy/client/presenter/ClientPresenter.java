package com.trajan.negentropy.client.presenter;

import com.trajan.negentropy.client.TaskEntry;
import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

@SpringComponent
@VaadinSessionScope
public interface ClientPresenter {
    void initListView(ListView listView);

    void setRootEntry(TaskEntry entry);

    TaskEntry getRootEntry();

    void initTaskInfoForm(TaskInfoForm taskInfoForm);

    void insertTaskNode(Insert position, TaskNode node);

    boolean isValid();

    void deleteNode(TaskNode node);

    Task saveTask();

    void deleteTask(Task task);

    void setTaskBean(Task data);

    void clearSelectedTask();
}