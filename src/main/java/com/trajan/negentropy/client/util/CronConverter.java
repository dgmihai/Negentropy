package com.trajan.negentropy.client.util;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.springframework.scheduling.support.CronExpression;

@SpringComponent
public class CronConverter implements Converter<String, CronExpression> {
    @Override
    public Result<CronExpression> convertToModel(String value, ValueContext context) {
        try {
            return Result.ok((value.isBlank())
                    ? null
                    : CronExpression.parse(value));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }

    }

    @Override
    public String convertToPresentation(CronExpression value, ValueContext context) {
        return (value == null)
                ? ""
                : value.toString();
    }
}
