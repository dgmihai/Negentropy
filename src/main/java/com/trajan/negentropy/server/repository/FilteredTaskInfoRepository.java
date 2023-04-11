package com.trajan.negentropy.server.repository;

import com.trajan.negentropy.server.entity.TaskInfo;
import jakarta.transaction.Transactional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Repository
@Transactional
public class FilteredTaskInfoRepository implements GenericSpecificationProvider<TaskInfo> {
    @Autowired
    private TaskInfoRepository taskInfoRepository;

    public List<TaskInfo> findByFilters(List<Filter> filters) {
        if (filters.size() > 0) {
            Specification<TaskInfo> spec = getSpecificationFromFilters(filters, TaskInfo.class);
            return taskInfoRepository.findAll(spec);
        } else {
            return taskInfoRepository.findAll();
        }
    }


}