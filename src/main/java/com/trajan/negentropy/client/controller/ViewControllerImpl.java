package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.client.controller.event.ListViewEvent;
import com.trajan.negentropy.client.controller.event.TaskInfoFormEvent;
import com.trajan.negentropy.client.view.ListView;
import com.trajan.negentropy.client.view.TaskInfoForm;
import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskRelationship;
import com.trajan.negentropy.server.service.TagService;
import com.trajan.negentropy.server.service.TaskInfoService;
import com.trajan.negentropy.server.service.TaskRelationshipService;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@UIScope
public class ViewControllerImpl implements ViewController {
    private static final Logger logger = LoggerFactory.getLogger(ViewControllerImpl.class);

    @Autowired
    private TaskInfoService taskInfoService;
    @Autowired
    private TaskRelationshipService taskRelationshipService;
    @Autowired
    private TagService tagService;

    private ListView listView;
    private TaskInfoForm taskInfoForm;

    @Override
    @EventListener
    @Transactional
    public void handleTaskInfoFormEvent_Save(TaskInfoFormEvent.SaveEvent saveEvent) {
        listView.getStatus().setText(
                taskInfoService.save(saveEvent.getData()).message());
        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
                listView,
                new ArrayList<>()));
    }

    @Override
    @EventListener
    public void handleTaskInfoFormEvent_Clear(TaskInfoFormEvent.ClearEvent clearEvent) {
        logger.info("{} - NO-OP", this.getClass());
        listView.getStatus().setText("Not yet implemented.");
    }

    @Override
    @EventListener
    @Transactional
    public void handleTaskInfoFormEvent_Delete(TaskInfoFormEvent.DeleteEvent deleteEvent) {
        listView.getStatus().setText(
                taskInfoService.delete(deleteEvent.getData()).message());
        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
                listView,
                new ArrayList<>()));
    }

    @Override
    @EventListener
    public void handleListViewEvent_Save(ListViewEvent.SaveEvent saveEvent) {
        TaskRelationship taskRelationship = saveEvent.getData();
        TaskInfo taskInfo = taskRelationship.getTaskInfo();

        if (taskRelationship.getOrderIndex() == -1) {
            taskRelationship.setOrderIndex(taskInfo.getRelationships().size());
        }
        taskInfo.addRelationship(taskRelationship);
        listView.getStatus().setText(
                taskInfoService.save(taskInfo).message());
        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
                listView,
                new ArrayList<>()));
    }

    @Override
    @EventListener
    public void handleListViewEvent_Remove(ListViewEvent.RemoveEvent removeEvent) {
        TaskRelationship taskRelationship = removeEvent.getData();
        TaskInfo taskInfo = taskRelationship.getTaskInfo();

        taskInfo.removeRelationship(taskRelationship);
        listView.getStatus().setText(
                taskInfoService.save(taskInfo).message());
        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
                listView,
                new ArrayList<>()));
    }

    @Override
    @EventListener
    public void handleListViewEvent_Update(ListViewEvent.UpdateEvent updateEvent) {
        if(this.listView == null) {
            this.listView = updateEvent.getSource();
            this.taskInfoForm = this.listView.getTaskInfoForm();
        }
        List<TaskRelationship> roots = taskRelationshipService.findRoots();
        if(roots.isEmpty()) listView.getStatus().setText("Failed to get any root items!");
        listView.getTreeGrid().setItems(roots, TaskRelationship::getChildRelationships);
    }
}
