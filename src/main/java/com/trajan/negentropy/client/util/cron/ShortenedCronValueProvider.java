package com.trajan.negentropy.client.util.cron;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.scheduling.support.CronExpression;

@SpringComponent
public class ShortenedCronValueProvider implements ValueProvider<CronExpression, String> {
    @Override
    public String apply(CronExpression cronExpression) {
        return (cronExpression == null)
                ? K.SHORTENED_CRON_PLACEHOLDER
                : cronExpression.toString().substring( 4);
    }
}
