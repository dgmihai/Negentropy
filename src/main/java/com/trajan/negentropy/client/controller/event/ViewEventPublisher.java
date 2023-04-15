package com.trajan.negentropy.client.controller.event;

import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.vaadin.flow.spring.annotation.UIScope;

import java.util.List;

@UIScope
public interface ViewEventPublisher {

    void publishTaskInfoFormEvent_Save(TaskInfoForm source, TaskInfo data);

    void publishTaskInfoFormEvent_Delete(TaskInfoForm source, TaskInfo data);

    void publishTaskInfoFormEvent_Clear(TaskInfoForm source);

    void publishListViewEvent_Save(ListView source, TaskNode data);

    void publishListViewEvent_Delete(ListView source, TaskNode data);

    void publishListViewEvent_Update(ListView source, TaskInfoForm form, List<Filter> filters);
}
