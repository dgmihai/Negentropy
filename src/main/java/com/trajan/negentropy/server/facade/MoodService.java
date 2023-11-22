package com.trajan.negentropy.server.facade;

import com.trajan.negentropy.model.Mood;
import com.trajan.negentropy.model.entity.Emotion;
import com.trajan.negentropy.model.entity.MoodEntity;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.repository.MoodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@Slf4j
public class MoodService implements ServiceFacade<Mood> {
    @Autowired private DataContext dataContext;
    @Autowired private MoodRepository moodRepository;

    public Mood persist(Mood mood) {
        return dataContext.toDO(dataContext.merge(mood));
    }

    public void delete(Long id) {
        dataContext.deleteMood(id);
    }

    public Mood get(Long id) {
        return dataContext.toDO(moodRepository.getReferenceById(id));
    }

    public Stream<Mood> getAll() {
        return moodRepository.findAll().stream().map(dataContext::toDO);
    }

    public Mood getLastMood() {
        MoodEntity mood =  moodRepository.findTopByOrderByTimestampDesc();
        return mood == null ? new Mood(Emotion.FRUSTRATION_IRRITATION_IMPATIENCE) : dataContext.toDO(mood);
    }
}

