package com.trajan.negentropy.client.session;

import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.server.facade.RoutineService;
import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Stream;

@SpringComponent
@VaadinSessionScope
@Accessors(fluent = true)
public class RoutineDataProvider extends AbstractDataProvider<Routine, Set<TimeableStatus>> {

    @Autowired private RoutineService routineService;

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public int size(Query<Routine, Set<TimeableStatus>> query) {
        return (int) routineService.countCurrentRoutines(query.getFilter().orElse(Set.of()));
    }

    @Override
    public Stream<Routine> fetch(Query<Routine, Set<TimeableStatus>> query) {
        return routineService.fetchRoutines(query.getFilter().orElse(Set.of()));
    }

    @Override
    public void refreshAll() {
        super.refreshAll();
    }
}
