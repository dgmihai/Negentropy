package com.trajan.negentropy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stressor_timestamps")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StressorTimestamp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stressor_id", nullable = false)
    private StressorEntity stressor;

    private LocalDateTime timestamp;
}
