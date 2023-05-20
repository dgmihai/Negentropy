package com.trajan.negentropy.client.controller.data;

import com.trajan.negentropy.server.backend.entity.status.RoutineStatus;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.model.Routine;
import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
public class RoutineDataProvider extends AbstractDataProvider<Routine, Set<RoutineStatus>> {
    private static final Logger logger = LoggerFactory.getLogger(RoutineDataProvider.class);

    @Autowired private RoutineService routineService;

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public int size(Query<Routine, Set<RoutineStatus>> query) {
        return (int) routineService.countCurrentRoutines(query.getFilter().orElse(Set.of()));
    }

    @Override
    public Stream<Routine> fetch(Query<Routine, Set<RoutineStatus>> query) {
        return routineService.fetchRoutines(query.getFilter().orElse(Set.of()));
    }
}
