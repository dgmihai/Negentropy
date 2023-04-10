package com.trajan.negentropy.data.repository;

import com.trajan.negentropy.data.entity.Tag;
import com.trajan.negentropy.data.entity.Task;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.jpa.domain.Specification.where;

@Component
@Getter
@Scope("prototype")
public class FilteredRepository {
    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected TagRepository tagRepository;

    public List<Task> getTaskQueryResult(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<Task> spec = getTaskSpecificationFromFilters(filters);
            return taskRepository.findAll(spec);
        } else {
            return taskRepository.findAll();
        }
    }

    public List<Tag> getTagQueryResult(List<Filter> filters) {
        if (filters.size() > 0) {
            return tagRepository.findAll(getTagSpecificationFromFilters(filters));
        } else {
            return tagRepository.findAll();
        }
    }

    protected Specification<Task> getTaskSpecificationFromFilters(List<Filter> filter) {
        Specification<Task> specification = where(createSpecification(filter.remove(0)));
        for (Filter input : filter) {
            specification = specification.and(createSpecification(input));
        }
        return specification;
    }

    protected Specification<Tag> getTagSpecificationFromFilters(List<Filter> filter) {
        Specification<Tag> specification = where(createSpecification(filter.remove(0)));
        for (Filter input : filter) {
            specification = specification.and(createSpecification(input));
        }
        return specification;
    }

    protected <T> Specification<T> createSpecification(Filter input) {
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
            default -> throw new RuntimeException("Operation not supported yet");
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
