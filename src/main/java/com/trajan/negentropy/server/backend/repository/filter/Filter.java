package com.trajan.negentropy.server.backend.repository.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class Filter {
    private String field;
    private QueryOperator operator;
    private Object value;
    private List<Object> values; //Used in case of IN operator
}