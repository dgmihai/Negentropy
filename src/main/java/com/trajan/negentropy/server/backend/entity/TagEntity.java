package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@RequiredArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Accessors(fluent = true)
@Getter
@Setter
public class TagEntity extends AbstractEntity {
    @NotBlank
    private String name;

    @ToString.Exclude
    @ManyToMany(
            mappedBy = "tags")
    @Singular
    private Set<TaskEntity> tasks = new LinkedHashSet<>();

    public TagEntity(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TagEntity(" + super.toString() + ", name:"  + this.name + ")";
    }
}
