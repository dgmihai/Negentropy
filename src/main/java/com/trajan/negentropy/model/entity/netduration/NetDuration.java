package com.trajan.negentropy.model.entity.netduration;

import com.trajan.negentropy.model.entity.TaskEntity;
import com.trajan.negentropy.server.backend.sync.NetDurationSyncListener;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.Duration;

@Entity
@EntityListeners(NetDurationSyncListener.class)
@Table(name = "net_durations")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
@IdClass(NetDurationID.class)
public class NetDuration {
    @Id
    @ManyToOne(cascade = {
            CascadeType.PERSIST,
            CascadeType.MERGE})
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    @Id
    private int importance = 0;

    private Duration netDuration = Duration.ZERO;
}