package com.trajan.negentropy.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Getter
@Setter
public class Tag extends AbstractEntity {
    @NotBlank
    private String name;
    @ManyToMany(
            mappedBy = "tags",
            fetch = FetchType.EAGER)
    private Set<Task> tasks = new LinkedHashSet<>();
}
