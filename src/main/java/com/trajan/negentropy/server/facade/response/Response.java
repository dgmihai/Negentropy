package com.trajan.negentropy.server.facade.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@RequiredArgsConstructor
@Getter
public class Response {
        protected final boolean success;
        protected final String message;
}
