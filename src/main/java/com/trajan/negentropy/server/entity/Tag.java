package com.trajan.negentropy.server.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class Tag extends AbstractEntity {
    @NotBlank
    private String name;

    @ToString.Exclude
    @ManyToMany(
            mappedBy = "tags")
    private Set<Task> tasks = new LinkedHashSet<>();
}
