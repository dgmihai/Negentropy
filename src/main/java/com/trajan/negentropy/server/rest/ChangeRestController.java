package com.trajan.negentropy.server.rest;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.InsertIntoChange;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.server.facade.ChangeService;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.response.Request;
import com.trajan.negentropy.server.facade.response.Response.DataMapResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/tasks")
@Slf4j
@Benchmark
public class ChangeRestController {
    @Autowired private ChangeService changeService;
    @Autowired private QueryService queryService;

    public static final TaskID STILL_TO_PLAN_ID = new TaskID(302); // Still to Plan

    @PostMapping("")
    public ResponseEntity<Task> add(@RequestParam String name) {
        try {
            Change persist = new PersistChange<>(new Task()
                    .name(name.trim()));
            Task stillToPlan = queryService.fetchTask(STILL_TO_PLAN_ID);

            DataMapResponse response = changeService.execute(persist);
            Task task;
            if (!response.success()) {
                Optional<Task> searchResult = queryService.fetchAllTasks(new TaskTreeFilter()
                        .name(name.trim()))
                        .filter(t -> t.name().equals(name.trim()))
                        .findFirst();
                if (searchResult.isPresent()) {
                    task = searchResult.get();
                } else {
                    return ResponseEntity.internalServerError()
                            .build();
                }
            } else {
                task = (Task) response.changeRelevantDataMap().get(persist.id()).get(0);
            }

            Change insertNode = new InsertIntoChange(
                    new TaskNodeDTO()
                            .childId(task.id()),
                    STILL_TO_PLAN_ID,
                    InsertLocation.FIRST);

            TaskNode resultNode = (TaskNode) changeService.execute(Request.of(insertNode))
                    .changeRelevantDataMap().get(persist.id()).get(0);
            return ResponseEntity.ok(resultNode.task());
        } catch (Exception e) {
            log.error("Error", e);
            return ResponseEntity.internalServerError()
                    .build();
        }
    }
}
