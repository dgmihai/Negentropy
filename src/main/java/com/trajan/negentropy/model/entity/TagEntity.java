package com.trajan.negentropy.model.entity;

import com.trajan.negentropy.model.data.TagData;
import com.trajan.negentropy.server.backend.sync.SyncManagerListener;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@EntityListeners(SyncManagerListener.class)
@Table(name = "tags")
@NoArgsConstructor
@Getter
@Setter
public class TagEntity extends AbstractEntity implements TagData<TagEntity> {

    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(name = "tag_seq", sequenceName = "tag_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_seq")
    private Long id;
    
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ToString.Exclude
    @ManyToMany(
            fetch = FetchType.LAZY,
            mappedBy = "tags")
    private Set<TaskEntity> tasks = new LinkedHashSet<>();

    @Override
    public String toString() {
        return "Tag[" + super.toString() + ", name="  + this.name + "]";
    }
}