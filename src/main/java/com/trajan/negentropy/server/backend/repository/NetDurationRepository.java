package com.trajan.negentropy.server.backend.repository;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.model.entity.netduration.NetDuration;
import com.trajan.negentropy.model.entity.netduration.NetDurationID;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NetDurationRepository extends BaseRepository<NetDuration, NetDurationID> {
    List<NetDuration> findByTaskIn(Collection<TaskEntity> tasks);
}
