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
public class TenetService implements ServiceFacade<Tenet> {
    @Autowired private DataContext dataContext;
    @Autowired private TenetRepository tenetRepository;

    public Tenet persist(Tenet tenet) {
        return dataContext.toDO(dataContext.merge(tenet));
    }

    public void delete(Long id) {
        dataContext.deleteTenet(id);
    }

    public Tenet get(Long id) {
        return dataContext.toDO(tenetRepository.getReferenceById(id));
    }

    public Tenet getRandom() {
        long count = tenetRepository.count();
        if (count == 0) {
            throw new IllegalStateException("No tenets found - is a DB properly configured?");
        }
        int random = (int) (Math.random() * count);
        return dataContext.toDO(tenetRepository.findAll().get(random));
    }

    public Stream<Tenet> getAll() {
        return tenetRepository.findAll().stream().map(dataContext::toDO);
    }
}
