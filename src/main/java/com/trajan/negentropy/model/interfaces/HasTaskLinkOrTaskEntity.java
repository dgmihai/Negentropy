package com.trajan.negentropy.model.interfaces;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.TaskLink;

import java.time.Duration;
import java.util.Optional;

public interface HasTaskLinkOrTaskEntity extends HasDuration, Named {
    Optional<TaskLink> link();
    TaskEntity task();

    default Duration duration() {
        return link().isPresent() ? link().get().duration() : task().duration();
    }
}
