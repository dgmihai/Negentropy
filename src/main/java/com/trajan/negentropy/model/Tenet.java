package com.trajan.negentropy.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@Setter
@Getter
public class Tenet implements Serializable {
    private final Long id;
    private String body;

    public Tenet(String body) {
        this.id = null;
        this.body = body;
    }

    @Override
    @JsonValue
    public String toString() {
        return body;
    }
}