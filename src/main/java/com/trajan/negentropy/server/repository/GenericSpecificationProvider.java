package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.AbstractEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public interface GenericSpecificationProvider<T extends AbstractEntity> {

    default Specification<T> getSpecificationFromFilters(List<Filter> filters, Class<T> entityType) {
        Specification<T> specification = Specification.where(createSpecification(filters.remove(0), entityType));
        for (Filter input : filters) {
            specification = specification.and(createSpecification(input, entityType));
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
                            (Number) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
            case LESS_THAN -> (root, query, criteriaBuilder) ->
                    criteriaBuilder.lt(root.get(input.getField()),
                            (Number) castToRequiredType(root.get(input.getField()).getJavaType(), input.getValue()));
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

        if (fieldType.isAssignableFrom(value.getClass())) {
            return value;
        } else if (String.class.isAssignableFrom(fieldType)) {
            return value.toString();
        } else if (BigDecimal.class.isAssignableFrom(fieldType)) {
            try {
                return new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (Boolean.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        } else if (Byte.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Byte.parseByte((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (Short.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Short.parseShort((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (Integer.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (Long.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Long.parseLong((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (Float.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Float.parseFloat((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (Double.class.isAssignableFrom(fieldType)) {
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else if (byte[].class.isAssignableFrom(fieldType)) {
            if (value instanceof byte[]) {
                return value;
            }
        } else if (java.sql.Date.class.isAssignableFrom(fieldType)) {
            if (value instanceof java.util.Date) {
                return new java.sql.Date(((java.util.Date) value).getTime());
            } else if (value instanceof String) {
                try {
                    return java.sql.Date.valueOf((String) value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        } else if (java.sql.Time.class.isAssignableFrom(fieldType)) {
            if (value instanceof java.util.Date) {
                return new java.sql.Time(((java.util.Date) value).getTime());
            } else if (value instanceof String) {
                try {
                    return java.sql.Time.valueOf((String) value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        } else if (java.sql.Timestamp.class.isAssignableFrom(fieldType)) {
            if (value instanceof java.util.Date) {
                return new java.sql.Timestamp(((java.util.Date) value).getTime());
            } else if (value instanceof String) {
                try {
                    return java.sql.Timestamp.valueOf((String) value);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private Object castToRequiredType(Class<?> fieldType, List<Object> value) {
        List<Object> lists = new ArrayList<>();
        for (Object s : value) {
            lists.add(castToRequiredType(fieldType, s));
        }
        return lists;
    }
}
