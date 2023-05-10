package com.trajan.negentropy.server.backend;

import com.trajan.negentropy.server.backend.entity.TagEntity;
import com.trajan.negentropy.server.backend.repository.TagRepository;
import com.trajan.negentropy.server.facade.model.id.TagID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    @Autowired private TagRepository tagRepository;

    public TagEntity getTagEntity(TagID id) {
        return tagRepository.findById(id.val()).orElseThrow();
    }



    public List<TagEntity> findAll() {
        return tagRepository.findAll();
    }

    public void delete(TagID id) {
        tagRepository.deleteById(id.val());
    }
}