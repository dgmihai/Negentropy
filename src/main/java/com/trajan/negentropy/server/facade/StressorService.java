package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Stressor;
import com.trajan.negentropy.model.entity.StressorTimestamp;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.repository.StressorRepository;
import com.trajan.negentropy.server.backend.repository.StressorTimestampRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
@Slf4j
public class StressorService implements ServiceFacade<Stressor> {
    @Autowired private DataContext dataContext;
    @Autowired private StressorRepository stressorRepository;
    @Autowired private StressorTimestampRepository stressorTimestampRepository;

    public Stressor persist(Stressor stressor) {
        return dataContext.toDO(dataContext.merge(stressor));
    }

    public void record(Long id, LocalDateTime timestamp) {
        stressorTimestampRepository.save(new StressorTimestamp()
                .stressor(stressorRepository.getReferenceById(id))
                .timestamp(timestamp));
    }

    public void delete(Long id) {
        dataContext.deleteStressor(id);
    }

    public Stressor get(Long id) {
        return dataContext.toDO(stressorRepository.getReferenceById(id));
    }

    public Stream<Stressor> getAll() {
        return stressorRepository.findAll().stream().map(dataContext::toDO);
    }
}
