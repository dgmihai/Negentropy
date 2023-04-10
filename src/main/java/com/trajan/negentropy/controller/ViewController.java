package com.trajan.negentropy.controller;

import com.trajan.negentropy.data.entity.Task;
import com.trajan.negentropy.data.entity.Task_;
import com.trajan.negentropy.data.repository.Filter;
import com.trajan.negentropy.data.repository.QueryOperator;
import com.trajan.negentropy.data.service.DataService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Component
public class ViewController {
    @Autowired
    private DataService service;

    private Filter textFilter;

    private Filter rootFilter = Filter.builder()
            .field(Task_.PARENTS)
            .operator(QueryOperator.EMPTY)
            .value("")
            .build();

    public List<Task> findTasks() {
        return this.findTasks(new ArrayList<>());
    }

    public List<Task> findTasks(Filter filter) {
        return this.findTasks(new ArrayList<>(Collections.singletonList(filter)));
    }

    public List<Task> findTasks(List<Filter> filters) {
        if (textFilter != null) {
                filters.add(textFilter);
        }
        return service.findTasks(filters);
    }

    public List<Task> findRootTasks() {
        return this.findTasks(rootFilter);
    }

    public void setFilterText(String filterText) {
        textFilter = Filter.builder()
                .value(filterText)
                .operator(QueryOperator.IN)
                .field(Task_.NAME)
                .build();
    }

    @Transactional
    public void saveTask(Task task) {
        service.saveTask(task);
        if (task.getAddToChildrenOf() != null) {
            task.getAddToChildrenOf().getChildren().add(task);
            service.saveTask(task.getAddToChildrenOf());
        }
        if (task.getRemoveFromChildrenOf() != null) {
            task.getRemoveFromChildrenOf().getChildren().remove(task);
            service.saveTask(task.getRemoveFromChildrenOf());
        }
    }
}
