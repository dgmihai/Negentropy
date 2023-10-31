package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Tenet;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.repository.TenetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@Slf4j
public class TenetService {
    @Autowired private DataContext dataContext;
    @Autowired private TenetRepository tenetRepository;

    public Tenet persist(Tenet tenet) {
        return dataContext.toDO(dataContext.mergeTenet(tenet));
    }

    public void delete(Long id) {
        dataContext.deleteTenet(id);
    }

    public Tenet get(Long id) {
        return dataContext.toDO(tenetRepository.getReferenceById(id));
    }

    public Tenet getRandom() {
        long count = tenetRepository.count();
        int random = (int) (Math.random() * count);
        return dataContext.toDO(tenetRepository.findAll().get(random));
    }

    public Stream<Tenet> getAll() {
        return tenetRepository.findAll().stream().map(dataContext::toDO);
    }
}
