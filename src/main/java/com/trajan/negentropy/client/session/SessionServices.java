package com.trajan.negentropy.client.session;

import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.facade.*;
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
    @Autowired private MoodService mood;
    @Autowired private RecordService record;
}
