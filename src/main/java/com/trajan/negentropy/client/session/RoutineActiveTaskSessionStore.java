package com.trajan.negentropy.client.session;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.client.logger.SessionLogger;
import com.trajan.negentropy.model.TaskNode;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.Routine;
import com.trajan.negentropy.model.entity.routine.RoutineStep;
import com.trajan.negentropy.model.id.RoutineID;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

@SpringComponent
@VaadinSessionScope
@Benchmark
public class RoutineActiveTaskSessionStore {
    private final SessionLogger logger = new SessionLogger();

    @Autowired private SessionServices services;

    private Registration broadcasterRegistration;
    private Multimap<RoutineID, TaskNode> nodesThatHaveActiveSteps = ArrayListMultimap.create();

    @PostConstruct
    public void init() {
        broadcasterRegistration = services.routine().register(this::update);
        services.query().fetchActiveRoutines().forEach(this::update);
    }

    @PreDestroy
    public void destroy() {
        broadcasterRegistration.remove();
    }

    public synchronized void update(Routine routine) {
        nodesThatHaveActiveSteps.removeAll(routine.id());
        nodesThatHaveActiveSteps.putAll(routine.id(), routine.steps().values().stream()
                .filter(step -> step.status().equalsAny(TimeableStatus.ACTIVE, TimeableStatus.DESCENDANT_ACTIVE)
                        || routine.currentStep().equals(step))
                .filter(step -> step.nodeOptional().isPresent())
                .map(RoutineStep::node)
                .peek(node -> logger.debug("<" + node.name() + "> - Active Step"))
                .toList());
    }

    public synchronized Collection<TaskNode> nodesThatHaveActiveSteps() {
        return nodesThatHaveActiveSteps.values();
    }
}
