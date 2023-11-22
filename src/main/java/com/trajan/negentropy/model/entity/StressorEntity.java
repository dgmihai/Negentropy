package com.trajan.negentropy.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stressors")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StressorEntity {

    @Id
    @SequenceGenerator(name = "stressor_seq", sequenceName = "stressor_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stressor_seq")
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "stressor",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<StressorTimestamp> timestamps = new HashSet<>();

    @Override
    public String toString() {
        return name;
    }
}
