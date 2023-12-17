package com.trajan.negentropy.server;

import com.trajan.negentropy.model.sync.Change.MergeChange;
import com.trajan.negentropy.server.facade.response.Request;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoutineTestTemplateWithRequiredTasks extends RoutineTestTemplate {

    @Override
    protected void init() {
        super.init();

        changeService.execute(Request.of(
                new MergeChange<>(tasks.get(TWOONE)
                        .required(true)),
                new MergeChange<>(tasks.get(TWOTWOTHREE_AND_THREETWOTWO)
                        .required(true)),
                new MergeChange<>(tasks.get(THREEONE)
                        .required(true)),
                new MergeChange<>(nodes.get(Triple.of(NULL, TWO, 1))
                        .projectDurationLimit(Optional.empty()))));
        refreshMaps();
    }
}
