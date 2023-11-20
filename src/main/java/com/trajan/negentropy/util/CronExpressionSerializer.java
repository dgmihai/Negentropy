package com.trajan.negentropy.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.trajan.negentropy.client.util.cron.ShortenedCronValueProvider;
import org.springframework.scheduling.support.CronExpression;

import java.io.IOException;

public class CronExpressionSerializer extends JsonSerializer<CronExpression> {
    private final static ShortenedCronValueProvider shortenedCronValueProvider = new ShortenedCronValueProvider();

    @Override
    public void serialize(CronExpression value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeString(shortenedCronValueProvider.apply(value));
        }
    }
}

