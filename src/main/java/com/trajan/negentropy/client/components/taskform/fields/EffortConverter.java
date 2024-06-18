package com.trajan.negentropy.client.components.taskform.fields;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class EffortConverter implements Converter<String, Integer> {
    public static final String DEFAULT_EFFORT = "-";

    @Override
    public Result<Integer> convertToModel(String value, ValueContext context) {
        return toModel(value);
    }

    public static Result<Integer> toModel(String value) {
        if (value == null || value.isBlank() || value.equals(DEFAULT_EFFORT)) {
            return Result.ok(-1);
        } else {
            return Result.ok(Integer.parseInt(value));
        }
    }

    @Override
    public String convertToPresentation(Integer value, ValueContext context) {
        return toPresentation(value);
    }

    public static String toPresentation(Integer value) {
        if (value == null || value == -1) {
            return DEFAULT_EFFORT;
        } else {
            return value.toString();
        }
    }
}
