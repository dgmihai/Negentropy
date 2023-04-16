package com.trajan.negentropy.client.controller.event;

import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.repository.filter.Filter;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@UIScope
@Component
public class ViewEventPublisherImpl implements ViewEventPublisher {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishTaskInfoFormEvent_Save(TaskInfoForm source, Task data) {
        applicationEventPublisher.publishEvent(new TaskInfoFormEvent.SaveEvent(source, data));
    }

    @Override
    public void publishTaskInfoFormEvent_Delete(TaskInfoForm source, Task data) {
        applicationEventPublisher.publishEvent(new TaskInfoFormEvent.DeleteEvent(source, data));
    }

    @Override
    public void publishTaskInfoFormEvent_Clear(TaskInfoForm source) {
        applicationEventPublisher.publishEvent(new TaskInfoFormEvent.ClearEvent(source));
    }

    @Override
    public void publishListViewEvent_Save(ListView source, TaskNode data) {
        applicationEventPublisher.publishEvent(new ListViewEvent.SaveEvent(source, data));
    }

    @Override
    public void publishListViewEvent_Delete(ListView source, TaskNode data) {
        applicationEventPublisher.publishEvent(new ListViewEvent.RemoveEvent(source, data));
    }

    @Override
    public void publishListViewEvent_Update(ListView source, TaskInfoForm form, List<Filter> filters) {
        applicationEventPublisher.publishEvent(new ListViewEvent.UpdateEvent(source, filters));
    }
}