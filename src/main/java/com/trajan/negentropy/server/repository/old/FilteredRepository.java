package com.trajan.negentropy.server.repository.old;

import com.trajan.negentropy.server.entity.AbstractEntity;
import com.trajan.negentropy.server.repository.GenericSpecificationProvider;
import lombok.Getter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Getter
@Scope("prototype")
public class FilteredRepository implements GenericSpecificationProvider<AbstractEntity> {
//    @Autowired
//    protected TaskInfoRepository taskInfoRepository;
//    @Autowired
//    protected TaskRelationshipRepository taskRelationshipRepository;
//    @Autowired
//    protected TagRepository tagRepository;

//    public List<TaskInfo> getTaskInfoQueryResult(List<Filter> filters) {
//        if (filters.size() > 0) {
//            Specification<TaskInfo> spec = getTaskInfoSpecificationFromFilters(filters);
//            return taskInfoRepository.findAll(spec);
//        } else {
//            return taskInfoRepository.findAll();
//        }
//    }
//
//    public List<TaskRelationship> getTaskRelationshipQueryResult(List<Filter> filters) {
//        if (filters.size() > 0) {
//            Specification<TaskRelationship> spec = getTaskRelationshipSpecificationFromFilters(filters);
//            return taskRelationshipRepository.findAll(spec);
//        } else {
//            return taskRelationshipRepository.findAll();
//        }
//    }
//
//    public List<Tag> getTagQueryResult(List<Filter> filters) {
//        if (filters.size() > 0) {
//            return tagRepository.findAll(getTagSpecificationFromFilters(filters));
//        } else {
//            return tagRepository.findAll();
//        }
//    }

//    protected Specification<TaskInfo> getTaskInfoSpecificationFromFilters(List<Filter> filter) {
//        Specification<TaskInfo> specification = where(createSpecification(filter.remove(0)));
//        for (Filter input : filter) {
//            specification = specification.and(createSpecification(input));
//        }
//        return specification;
//    }
//
//    protected Specification<TaskRelationship> getTaskRelationshipSpecificationFromFilters(List<Filter> filter) {
//        Specification<TaskRelationship> specification = where(createSpecification(filter.remove(0)));
//        for (Filter input : filter) {
//            specification = specification.and(createSpecification(input));
//        }
//        return specification;
//    }
//
//    protected Specification<Tag> getTagSpecificationFromFilters(List<Filter> filter) {
//        Specification<Tag> specification = where(createSpecification(filter.remove(0)));
//        for (Filter input : filter) {
//            specification = specification.and(createSpecification(input));
//        }
//        return specification;
//    }
}
