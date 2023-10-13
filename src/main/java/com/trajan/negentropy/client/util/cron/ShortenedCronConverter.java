package com.trajan.negentropy.client.util.cron;

import com.trajan.negentropy.client.K;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.scheduling.support.CronExpression;

@SpringComponent
public class ShortenedCronConverter implements Converter<String, CronExpression> {
    @Override
    public Result<CronExpression> convertToModel(String value, ValueContext context) {
        try {
            if (value.trim().equalsIgnoreCase("d")) {
                return Result.ok(CronExpression.parse("@daily"));
            } else {
                return (value.isBlank())
                        ? Result.ok(CronExpression.parse(K.NULL_CRON))
                        : Result.ok(CronExpression.parse("0 0 " + value));
            }
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    public CronExpression convertToModel(String value) {
        return convertToModel(value, null).getOrThrow(IllegalArgumentException::new);
    }

    @Override
    public String convertToPresentation(CronExpression value, ValueContext context) {
        return (value == null)
                ? ""
                : value.toString().substring(4);
    }
}
