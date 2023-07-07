package com.trajan.negentropy.client.util;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.scheduling.support.CronExpression;

@SpringComponent
public class CronValueProvider implements ValueProvider<CronExpression, String> {
    @Override
    public String apply(CronExpression cronExpression) {
        return (cronExpression == null)
                ? K.CRON_PLACEHOLDER
                : cronExpression.toString();
    }
}
