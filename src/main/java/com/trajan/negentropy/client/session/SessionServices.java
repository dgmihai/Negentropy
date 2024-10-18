package com.trajan.negentropy.client.session;

import com.trajan.negentropy.server.backend.TagService;
import com.trajan.negentropy.server.facade.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;
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
    @Autowired private StressorService stressor;

    public static void ifNotMobile(Runnable runnable) {
        WebBrowser browser = VaadinSession.getCurrent().getBrowser();
        if (!browser.isIPhone() && !browser.isAndroid() && !browser.isWindowsPhone()) {
            runnable.run();
        }
    }

}
