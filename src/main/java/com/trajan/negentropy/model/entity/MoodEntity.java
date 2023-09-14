package com.trajan.negentropy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "moods")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@SequenceGenerator(name = "mood_seq", sequenceName = "mood_seq")
public class MoodEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mood_seq")
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Emotion emotion;
}
