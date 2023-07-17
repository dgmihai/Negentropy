package com.trajan.negentropy.server.facade.response;

import com.trajan.negentropy.client.K;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@RequiredArgsConstructor
@Getter
public class Response {
        protected final boolean success;
        protected final String message;

        public static Response OK() {
                return new Response(true, K.OK);
        }
}
