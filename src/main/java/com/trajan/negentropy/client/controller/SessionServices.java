package com.trajan.negentropy.client.controller;

import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.facade.ChangeService;
import com.trajan.negentropy.server.facade.QueryService;
import com.trajan.negentropy.server.facade.RoutineService;
import com.trajan.negentropy.server.facade.TenetService;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

@SpringComponent
@VaadinSessionScope
@Getter
public class SessionServices {
    @Autowired private QueryService query;
    @Autowired private ChangeService change;
    @Autowired private TagService tag;
    @Autowired private RoutineService routine;
    @Autowired private TenetService tenet;
}
