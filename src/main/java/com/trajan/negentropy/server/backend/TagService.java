package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.model.entity.TagEntity;
import com.trajan.negentropy.model.id.TagID;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.backend.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    @Autowired private TagRepository tagRepository;
    @Autowired private TaskRepository taskRepository;

    public TagEntity getTagEntity(TagID id) {
        return tagRepository.findById(id.val()).orElseThrow();
    }

    public List<TagEntity> findAll() {
        return tagRepository.findAll();
    }

    public void delete(TagID id) {
        tagRepository.deleteById(id.val());
    }

    public Optional<TagEntity> getTagEntityByName(String tagName) {
        return tagRepository.findFirstByName(tagName);
    }

    public Stream<TagEntity> getTagsForTask(TaskID taskId) {
        return tagRepository.findByTasksId(taskId.val());
    }

    public Set<TaskID> getTaskIdByTagId(TagID tagId) {
        return taskRepository.findTaskIdsByTagId(tagId.val())
                .map(TaskID::new)
                .collect(Collectors.toSet());
    }
}