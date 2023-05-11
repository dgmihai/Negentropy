package com.trajan.negentropy.server.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public class TagEntity extends AbstractEntity {
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @ToString.Exclude
    @ManyToMany(
            mappedBy = "tags")
    private Set<TaskEntity> tasks = new LinkedHashSet<>();

    @Override
    public String toString() {
        return "Tag[" + super.toString() + ", name="  + this.name + "]";
    }
}