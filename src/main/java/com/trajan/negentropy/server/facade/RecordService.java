package com.trajan.negentropy.server.facade;

import com.querydsl.core.BooleanBuilder;
import com.trajan.negentropy.aop.Benchmark;
import com.trajan.negentropy.model.Record;
import com.trajan.negentropy.model.RecordSpan;
import com.trajan.negentropy.model.Task;
import com.trajan.negentropy.model.entity.TimeableStatus;
import com.trajan.negentropy.model.entity.routine.QRoutineStepEntity;
import com.trajan.negentropy.model.entity.routine.RoutineStepEntity;
import com.trajan.negentropy.model.filter.NonSpecificTaskNodeTreeFilter;
import com.trajan.negentropy.model.id.TaskID;
import com.trajan.negentropy.server.backend.DataContext;
import com.trajan.negentropy.server.backend.repository.RoutineRepository;
import com.trajan.negentropy.server.backend.repository.RoutineStepRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@Benchmark
public class RecordService {
    @Autowired private RoutineRepository routineRepository;
    @Autowired private RoutineStepRepository routineStepRepository;
    @Autowired private QueryService queryService;
    @Autowired private DataContext dataContext;

    private static final QRoutineStepEntity Q_STEP = QRoutineStepEntity.routineStepEntity;

    public Stream<Record> fetchRecordsOnDay(LocalDate date) {
        Sort sort = Sort.by(Sort.Direction.ASC, "startTime");
        BooleanBuilder predicate = recordDatePredicate(date, date);
        return StreamSupport.stream(routineStepRepository.findAll(predicate, sort).spliterator(), true)
                .map(Record::new);
    }

    public Map<TaskID, RecordSpan> fetchRecordsDuringTimespan(LocalDate startDate, LocalDate endDate) {
        BooleanBuilder predicate = recordDatePredicate(startDate, endDate);
        Map<Task, Map<TimeableStatus, List<Record>>> recordMap = StreamSupport.stream(routineStepRepository.findAll(predicate).spliterator(), true)
                .collect(Collectors.groupingBy(
                        step -> dataContext.toDO(step.task()),
                        Collectors.groupingBy(
                                RoutineStepEntity::status,
                                Collectors.mapping(
                                        Record::new,
                                        Collectors.toList()))));
        Map<TaskID, RecordSpan> resultMap = new HashMap<>();
        for (Entry<Task, Map<TimeableStatus, List<Record>>> entry : recordMap.entrySet()) {
            int totalCount = 0;
            for (List<Record> records : entry.getValue().values()) {
                totalCount += records.size();
            }
            RecordSpan recordSpan = new RecordSpan(
                    entry.getKey(),
                    entry.getValue(),
                    totalCount,
                    calculateRecordSpanDurationAverage(entry.getValue().get(TimeableStatus.COMPLETED)),
                    queryService.fetchNetDuration(entry.getKey().id(), new NonSpecificTaskNodeTreeFilter()));
            resultMap.put(entry.getKey().id(), recordSpan);
        }
        return resultMap;
    }

    private BooleanBuilder recordDatePredicate(LocalDate startDate, LocalDate endDate) {
        BooleanBuilder predicate = new BooleanBuilder();
        if (startDate != null) {
            LocalDateTime startOfDay = startDate.atStartOfDay();
            predicate.and(Q_STEP.finishTime.after(startOfDay)
                    .or(Q_STEP.startTime.after(startOfDay)));
        }
        if (endDate == null) endDate = LocalDate.now();
        LocalDateTime endOfDay = endDate.plusDays(1).atStartOfDay();
        predicate.and(Q_STEP.finishTime.before(endOfDay)
                .or(Q_STEP.startTime.before(endOfDay)));
        return predicate;
    }

    public List<Record> filterDurations(List<Record> records) {
        if (records == null) return null;

        records = records.stream()
                .filter(record -> record.elapsedTime() != null)
                .filter(record -> record.elapsedTime().compareTo(Duration.ofSeconds(5)) > 0)
                .collect(Collectors.toList());

        int size = records.size();

        if (size > 3) {
            Duration q1 = records.get(size / 4).elapsedTime();
            Duration q3 = records.get(3 * size / 4).elapsedTime();

            // Calculate IQR (Inter-Quartile Range)
            Duration iqr = q3.minus(q1);

            // Calculate bounds for outliers
            Duration lowerBound = q1.minus(iqr.multipliedBy(3 / 2));
            Duration upperBound = q3.plus(iqr.multipliedBy(3 / 2));

            // Remove outliers
            records.removeIf(d -> d.elapsedTime().compareTo(lowerBound) < 0 || d.elapsedTime().compareTo(upperBound) > 0);
        }
        return records;
   }

    private Duration calculateRecordSpanDurationAverage(List<Record> records) {
        records = filterDurations(records);
        Duration sum = Duration.ZERO;
        if (records == null || records.isEmpty()) return null;
        for (Record record : filterDurations(records)) {
            sum = sum.plus(record.elapsedTime());
        }
        return sum.dividedBy(records.size());
    }
}
