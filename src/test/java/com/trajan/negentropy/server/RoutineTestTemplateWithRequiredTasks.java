package com.trajan.negentropy.server;

import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.TaskNodeDTO;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoutineTestTemplateWithRequiredTasks extends TaskTestTemplate {

    @Override
    protected void init() {
        initTasks(
                null,
                List.of(Pair.of(
                                new Task()
                                        .name(ONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWO)
                                        .duration(Duration.ofHours(2))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofHours(2))
                        ), Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .duration(Duration.ofHours(3))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofHours(10))
                        ), Pair.of(
                                new Task()
                                        .name(FOUR)
                                        .duration(Duration.ofHours(4)),
                                new TaskNodeDTO()
                        ),Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofHours(5))
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR)
                                        .duration(Duration.ofHours(6)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                TWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ),Pair.of(
                                new Task()
                                        .name(TWOTWO)
                                        .duration(Duration.ofHours(1))
                                        .project(true),
                                new TaskNodeDTO()
                                        .projectDuration(Duration.ofHours(2))
                        ), Pair.of(
                                new Task()
                                        .name(TWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                TWOTWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOTWOONE)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTWO)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1))
                                        .required(true),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                THREE_AND_FIVE,
                List.of(Pair.of(
                                new Task()
                                        .name(THREEONE)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        )
                )
        );

        initTasks(
                THREETWO,
                List.of(Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeDTO()
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR),
                                new TaskNodeDTO()
                        )
                )
        );

        refreshMaps();
    }
}
