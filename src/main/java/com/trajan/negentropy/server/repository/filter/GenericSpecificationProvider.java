package com.trajan.negentropy.server.repository.filter;

import com.trajan.negentropy.server.entity.AbstractEntity;
import com.trajan.negentropy.server.entity.Task;
import com.trajan.negentropy.server.entity.TaskNode;
import com.trajan.negentropy.server.entity.Task_;
import org.springframework.data.jpa.domain.Specification;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface GenericSpecificationProvider<T extends AbstractEntity> {

    // TODO: Better exception handling in lambda

    List<T> findByFilters(List<Filter> filters);

    default Specification<T> getSpecificationFromFilters(List<Filter> filters, Class<T> entityType) {
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
                case EQ_TASK -> (root, query, criteriaBuilder) -> {
                    if (root.get(input.getField()).getJavaType().equals(Task.class)) {
                        return criteriaBuilder.equal(root.get(input.getField()).get(Task_.ID),
                                getConversionFunction(Long.class).apply(input.getValue()));
                    } else throw new IllegalArgumentException("Field is not of type Task");
                };
                case NOT_EQ_TASK -> (root, query, criteriaBuilder) -> {
                    if (root.get(input.getField()).getJavaType().equals(Task.class)) {
                        return criteriaBuilder.notEqual(root.get(input.getField()).get(Task_.ID),
                        getConversionFunction(Long.class).apply(input.getValue()));
                    } else throw new IllegalArgumentException("Field is not of type Task");
                };
                case EQ_TASK_NODE -> (root, query, criteriaBuilder) -> {
                    if (root.get(input.getField()).getJavaType().equals(TaskNode.class)) {
                        return criteriaBuilder.equal(root.get(input.getField()).get(Task_.ID),
                                getConversionFunction(Long.class).apply(input.getValue()));
                    } else throw new IllegalArgumentException("Field is not of type TaskNode");
                };
                case NOT_EQ_TASK_NODE -> (root, query, criteriaBuilder) -> {
                    if (root.get(input.getField()).getJavaType().equals(TaskNode.class)) {
                        return criteriaBuilder.notEqual(root.get(input.getField()).get(Task_.ID),
                                getConversionFunction(Long.class).apply(input.getValue()));
                    } else throw new IllegalArgumentException("Field is not of type TaskNode");
                };
                case EQUALS -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get(input.getField()),
                                castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case NOT_EQ -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.notEqual(root.get(input.getField()),
                                castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case GREATER_THAN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.gt(root.get(input.getField()),
                                (Number) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case LESS_THAN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.lt(root.get(input.getField()),
                                (Number) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
                case SHORTER_THAN -> ((root, query, criteriaBuilder) ->
                        criteriaBuilder.lessThan(root.get(input.getField()),
                                (Duration) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue())));
                case LONGER_THAN -> (root, query, criteriaBuilder) ->
                        criteriaBuilder.greaterThan(root.get(input.getField()),
                                (Duration) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
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

        if (fieldType.equals(value.getClass())) {
            return value;
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
        conversionFunctions.put(Duration.class, obj -> (Duration) obj);
        conversionFunctions.put(Task.class, obj -> ((Task) obj).getId());
        conversionFunctions.put(TaskNode.class, obj -> ((TaskNode) obj).getId());

        return conversionFunctions.get(fieldType);
    }
}
