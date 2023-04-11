package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.event.ListViewEvent;
import com.trajan.negentropy.client.controller.event.TaskInfoFormEvent;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.context.event.EventListener;

@UIScope
public interface ViewController {
    @EventListener
    void handleTaskInfoFormEvent_Save(TaskInfoFormEvent.SaveEvent saveEvent);

    @EventListener
    void handleTaskInfoFormEvent_Clear(TaskInfoFormEvent.ClearEvent clearEvent);

    @EventListener
    void handleTaskInfoFormEvent_Delete(TaskInfoFormEvent.DeleteEvent deleteEvent);

    @EventListener
    void handleListViewEvent_Save(ListViewEvent.SaveEvent saveEvent);

    @EventListener
    void handleListViewEvent_Remove(ListViewEvent.RemoveEvent deleteEvent);

    @EventListener
    void handleListViewEvent_Update(ListViewEvent.UpdateEvent updateEvent);
}
