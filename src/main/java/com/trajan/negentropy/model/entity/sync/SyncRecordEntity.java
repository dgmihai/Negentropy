package com.trajan.negentropy.model.entity.sync;

import com.trajan.negentropy.model.id.ID.SyncID;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sync_records")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@SequenceGenerator(name = "sync_seq", sequenceName = "sync_seq")
public class SyncRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sync_seq")
    private Long id;

    private LocalDateTime timestamp = LocalDateTime.now();

    @OneToMany(fetch = FetchType.EAGER,
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ChangeRecordEntity> changes = new ArrayList<>();

    public SyncID id() {
        return id != null
                ? new SyncID(id)
                : null;
    }
}
