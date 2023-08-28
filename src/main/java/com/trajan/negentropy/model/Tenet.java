package com.trajan.negentropy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
public class Tenet {
    private final Long id;
    private String body;

    public Tenet(String body) {
        this.id = null;
        this.body = body;
    }

    @Override
    public String toString() {
        return body;
    }
}