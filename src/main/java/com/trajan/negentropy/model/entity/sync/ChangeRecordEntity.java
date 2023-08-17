package com.trajan.negentropy.model.entity.sync;

import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordDataType;
import com.trajan.negentropy.model.sync.ChangeRecord.ChangeRecordType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "changes")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SequenceGenerator(name = "change_seq", sequenceName = "change_seq")
public class ChangeRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "change_seq")
    private Long id;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private ChangeRecordType changeType;

    @Enumerated(EnumType.STRING)
    private ChangeRecordDataType dataType;

    private Long entityId;
}