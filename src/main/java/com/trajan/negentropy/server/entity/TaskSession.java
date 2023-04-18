package com.trajan.negentropy.server.entity;

import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "active_task")
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TaskSession extends AbstractEntity {
    private static final Logger logger = LoggerFactory.getLogger(TaskSession.class);

    @OneToOne
    @JoinColumn(name = "node_id", nullable = false)
    private TaskNode node;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime pauseTime;

    @Builder.Default
    private Duration totalPausedDuration = Duration.ZERO;
}
