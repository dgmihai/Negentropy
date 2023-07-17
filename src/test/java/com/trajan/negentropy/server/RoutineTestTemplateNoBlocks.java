package com.trajan.negentropy.server;

import com.trajan.negentropy.server.facade.model.Task;
import com.trajan.negentropy.server.facade.model.TaskNodeInfo;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoutineTestTemplateNoBlocks extends TaskTestTemplate {

    @Override
    protected void initTasks(String parent, List<Pair<Task, TaskNodeInfo>> children) {
        children.forEach(pair -> pair.getFirst().block(false));
        super.initTasks(parent, children);
    }

    @Override
    protected void init() {
        initTasks(
                null,
                List.of(Pair.of(
                                new Task()
                                        .name(ONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(TWO)
                                        .duration(Duration.ofHours(2))
                                        .project(true),
                                new TaskNodeInfo()
                                        .projectDuration(Duration.ofHours(2))
                        ), Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .duration(Duration.ofHours(3))
                                        .project(true),
                                new TaskNodeInfo()
                                        .projectDuration(Duration.ofHours(10))
                        ), Pair.of(
                                new Task()
                                        .name(FOUR)
                                        .duration(Duration.ofHours(4)),
                                new TaskNodeInfo()
                        ),Pair.of(
                                new Task()
                                        .name(THREE_AND_FIVE)
                                        .project(true),
                                new TaskNodeInfo()
                                        .projectDuration(Duration.ofHours(5))
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR)
                                        .duration(Duration.ofHours(6)),
                                new TaskNodeInfo()
                        )
                )
        );

        initTasks(
                TWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOONE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ),Pair.of(
                                new Task()
                                        .name(TWOTWO)
                                        .duration(Duration.ofHours(1))
                                        .project(true),
                                new TaskNodeInfo()
                                        .projectDuration(Duration.ofHours(2))
                        ), Pair.of(
                                new Task()
                                        .name(TWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        )
                )
        );

        initTasks(
                TWOTWO,
                List.of(Pair.of(
                                new Task()
                                        .name(TWOTWOONE)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTWO)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        )
                )
        );

        initTasks(
                THREE_AND_FIVE,
                List.of(Pair.of(
                                new Task()
                                        .name(THREEONE)
                                        .duration(Duration.ofMinutes(30)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(THREETHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        )
                )
        );

        initTasks(
                THREETWO,
                List.of(Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(TWOTWOTHREE_AND_THREETWOTWO)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(THREETWOONE_AND_THREETWOTHREE)
                                        .duration(Duration.ofHours(1)),
                                new TaskNodeInfo()
                        ), Pair.of(
                                new Task()
                                        .name(SIX_AND_THREETWOFOUR),
                                new TaskNodeInfo()
                        )
                )
        );

        refreshMaps();
    }
}
