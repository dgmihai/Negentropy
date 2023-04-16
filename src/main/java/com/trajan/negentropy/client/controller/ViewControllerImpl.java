//package com.trajan.negentropy.client.controller;
//
//import com.trajan.negentropy.client.controller.event.ListViewEvent;
//import com.trajan.negentropy.client.controller.event.TaskInfoFormEvent;
//import com.trajan.negentropy.client.view.ListView;
//import com.trajan.negentropy.client.view.TaskInfoForm;
//import com.trajan.negentropy.server.entity.Task;
//import com.trajan.negentropy.server.entity.TaskNode;
//import com.trajan.negentropy.server.service.TagService;
//import com.trajan.negentropy.server.service.impl.TaskServiceImpl;
//import com.vaadin.flow.spring.annotation.UIScope;
//import jakarta.transaction.Transactional;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//@UIScope
//public class ViewControllerImpl implements ViewController {
//    private static final Logger logger = LoggerFactory.getLogger(ViewControllerImpl.class);
//
//    @Autowired
//    private TaskServiceImpl taskService;
//    @Autowired
//    private TagService tagService;
//
//    private ListView listView;
//    private TaskInfoForm taskInfoForm;
//
//    @Override
//    @EventListener
//    @Transactional
//    public void handleTaskInfoFormEvent_Save(TaskInfoFormEvent.SaveEvent saveEvent) {
//        listView.getStatus().setText(
//                taskService.saveTaskInfo(saveEvent.getData()).message());
//        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
//                listView,
//                new ArrayList<>()));
//    }
//
//    @Override
//    @EventListener
//    public void handleTaskInfoFormEvent_Clear(TaskInfoFormEvent.ClearEvent clearEvent) {
//        logger.info("{} - NO-OP", this.getClass());
//        listView.getStatus().setText("Not yet implemented.");
//    }
//
//    @Override
//    @EventListener
//    @Transactional
//    public void handleTaskInfoFormEvent_Delete(TaskInfoFormEvent.DeleteEvent deleteEvent) {
//        listView.getStatus().setText(
//                taskService.deleteTaskInfo(deleteEvent.getData()).message());
//        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
//                listView,
//                new ArrayList<>()));
//    }
//
//    @Override
//    @EventListener
//    @Transactional
//    public void handleListViewEvent_Save(ListViewEvent.SaveEvent saveEvent) {
//        TaskNode taskNode = saveEvent.getData();
//        Task taskInfo = taskNode.getChild();
//        taskInfo.getNodes().add(taskNode);
//
//        listView.getStatus().setText(
//                taskService.saveTaskInfo(taskInfo).message());
//        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
//                listView,
//                new ArrayList<>()));
//    }
//
//    @Override
//    @EventListener
//    @Transactional
//    public void handleListViewEvent_Remove(ListViewEvent.RemoveEvent removeEvent) {
//        TaskNode taskRelationship = removeEvent.getData();
//        Task taskInfo = taskRelationship.getChild();
//
//        taskInfo.getNodes().remove(taskRelationship);
//        listView.getStatus().setText(
//                taskService.saveTaskInfo(taskInfo).message());
//        handleListViewEvent_Update(new ListViewEvent.UpdateEvent(
//                listView,
//                new ArrayList<>()));
//    }
//
//    @Override
//    @EventListener
//    public void handleListViewEvent_Update(ListViewEvent.UpdateEvent updateEvent) {
//        if(this.listView == null) {
//            this.listView = updateEvent.getSource();
//            this.taskInfoForm = this.listView.getTaskInfoForm();
//        }
//        List<TaskNode> roots = taskService.getRootTaskInfo().getChildren();
//        if(roots.isEmpty()) listView.getStatus().setText("Failed to get any root items!");
//        //listView.getTreeGrid().setItems(roots, TaskNode::getChildren);
//    }
//}
