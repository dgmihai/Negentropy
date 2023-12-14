package com.trajan.negentropy.server.rest;

import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.controller.util.InsertLocation;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import com.trajan.negentropy.model.filter.TaskTreeFilter;
import com.trajan.negentropy.model.sync.Change;
import com.trajan.negentropy.model.sync.Change.PersistChange;
import com.trajan.negentropy.model.sync.Change.ReferencedInsertIntoChange;
import com.trajan.negentropy.server.facade.ChangeService;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.response.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@Slf4j
@Benchmark
public class ChangeRestController {
    @Autowired private ChangeService changeService;
    @Autowired private QueryService queryService;

    public static final String STILL_TO_PLAN = "Still To Plan";

    @GetMapping("")
    public ResponseEntity<Task> random(@RequestParam String name) {
        try {
            Change persist = new PersistChange<>(new Task()
                    .name(name));
            Task stillToPlan = queryService.fetchAllTasks(new TaskTreeFilter()
                    .name(STILL_TO_PLAN))
                    .findFirst().orElseThrow();
            Change insertNode = new ReferencedInsertIntoChange(
                    new TaskNodeDTO(),
                    stillToPlan.id(),
                    InsertLocation.FIRST,
                    persist.id());
            return ResponseEntity.ok((Task) changeService.execute(Request.of(persist, insertNode))
                    .changeRelevantDataMap().get(persist.id()).get(0));
        } catch (Exception e) {
            log.error("Error", e);
            return ResponseEntity.internalServerError()
                    .build();
        }
    }
}
