package com.trajan.negentropy.server.repository.filter;

import com.trajan.negentropy.server.entity.AbstractEntity;
import com.trajan.negentropy.server.entity.TaskInfo;
import com.trajan.negentropy.server.entity.TaskNode;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GenericSpecificationProvider<T extends AbstractEntity> {

    // TODO: Better exception handling in lambda
    public Specification<T> getSpecificationFromFilters(List<Filter> filters, Class<T> entityType) {
        List<Filter> mutableList = new ArrayList<>(filters);
        Specification<T> specification = null;
        for (Filter input : filters) {
            if (specification == null) {
                specification = Specification.where(createSpecification(mutableList.remove(0), entityType));
            } else {
                specification = specification.and(createSpecification(input, entityType));
            }
        }
        return specification;
    }

    private Specification<T> createSpecification(Filter input, Class<T> entityType) {
            return switch (input.getOperator()) {
                case EQUALS -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(input.getField()),
                                castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case NOT_EQ -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.notEqual(root.get(input.getField()),
                                castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case GREATER_THAN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.gt(root.get(input.getField()),
                                (Long) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case LESS_THAN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.lt(root.get(input.getField()),
                                (Long) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case LIKE -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.like(root.get(input.getField()), "%" + input.getValue() + "%");
                case IN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.in(root.get(input.getField()))
                                .value(castToRequiredType(root.get(input.getField()).getJavaType(), input.getValues()));
                case NOT_NULL -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.isNotNull(root.get(input.getField()));
                case NULL -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.isNull(root.get(input.getField()));
                case NOT_EMPTY -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.isNotEmpty(root.get(input.getField()));
                case EMPTY -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.isEmpty(root.get(input.getField()));
                default -> throw new RuntimeException("Operation not supported.");
            };
    }

    private Object castToRequiredType(Class<?> fieldType, Object value) {
        if (value == null) {
            return null;
        }

        Function<Object, ?> conversionFunction = getConversionFunction(fieldType);
        if (conversionFunction == null) {
            throw new IllegalArgumentException("Unsupported field type: " + fieldType.getName());
        }

        return conversionFunction.apply(value);
    }

    private Function<Object, ?> getConversionFunction(Class<?> fieldType) {
        Map<Class<?>, Function<Object, ?>> conversionFunctions = new HashMap<>();
        conversionFunctions.put(String.class, Object::toString);
        conversionFunctions.put(Integer.class, obj -> Integer.parseInt(obj.toString()));
        conversionFunctions.put(int.class, obj -> Integer.parseInt(obj.toString()));
        conversionFunctions.put(Long.class, obj -> Long.parseLong(obj.toString()));
        conversionFunctions.put(long.class, obj -> Long.parseLong(obj.toString()));
        conversionFunctions.put(Double.class, obj -> Double.parseDouble(obj.toString()));
        conversionFunctions.put(double.class, obj -> Double.parseDouble(obj.toString()));
        conversionFunctions.put(Float.class, obj -> Float.parseFloat(obj.toString()));
        conversionFunctions.put(float.class, obj -> Float.parseFloat(obj.toString()));
        conversionFunctions.put(Boolean.class, obj -> Boolean.parseBoolean(obj.toString()));
        conversionFunctions.put(boolean.class, obj -> Boolean.parseBoolean(obj.toString()));
        conversionFunctions.put(LocalDate.class, obj -> LocalDate.parse(obj.toString()));
        conversionFunctions.put(LocalDateTime.class, obj -> LocalDateTime.parse(obj.toString()));
        conversionFunctions.put(LocalTime.class, obj -> LocalTime.parse(obj.toString()));
        conversionFunctions.put(Duration.class, obj -> ((Duration) obj).toMillis());
        conversionFunctions.put(TaskInfo.class, obj -> ((TaskInfo) obj).getId());
        conversionFunctions.put(TaskNode.class, obj -> ((TaskNode) obj).getId());

        return conversionFunctions.get(fieldType);
    }
}
