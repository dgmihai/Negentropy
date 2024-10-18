package com.trajan.negentropy.client.components.taskform.fields;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

public class EffortConverter implements Converter<String, Integer> {
    public static final String DEFAULT_EFFORT_STRING = "-";
    public static final int DEFAULT_EFFORT_INT = -1;

    @Override
    public Result<Integer> convertToModel(String value, ValueContext context) {
        return toModel(value);
    }

    public static Result<Integer> toModel(String value) {
        if (value == null || value.isBlank() || value.equals(DEFAULT_EFFORT_STRING)) {
            return Result.ok(DEFAULT_EFFORT_INT);
        } else {
            return Result.ok(Integer.parseInt(value));
        }
    }

    @Override
    public String convertToPresentation(Integer value, ValueContext context) {
        return toPresentation(value);
    }

    public static String toPresentation(Integer value) {
        if (value == null || value == DEFAULT_EFFORT_INT) {
            return DEFAULT_EFFORT_STRING;
        } else {
            return value.toString();
        }
    }
}
