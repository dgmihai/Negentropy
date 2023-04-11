package com.trajan.negentropy.server.service;

import com.vaadin.flow.spring.annotation.UIScope;
import lombok.SneakyThrows;

@UIScope
public record Response(Boolean ok, String message) {
    public Response() {
        this(true, "OK");
    }

    public Response(String errorMessage) {
        this(false, errorMessage);
    }

    @SneakyThrows
    public Response(Exception e) {
        this(e.getMessage());
        throw e;
    }
}