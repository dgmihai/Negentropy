package com.trajan.negentropy.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Stressor {
    private Long id;
    private String name;

    public Stressor(String name) {
        this.id = null;
        this.name = name;
    }
}