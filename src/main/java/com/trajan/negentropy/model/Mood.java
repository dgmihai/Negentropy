package com.trajan.negentropy.model;

import com.trajan.negentropy.model.entity.Emotion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Mood {
    private Long id;
    private Emotion emotion;
    private LocalDateTime timestamp;

    public Mood(Emotion emotion) {
        this.emotion = emotion;
        this.timestamp = LocalDateTime.now();
    }
}