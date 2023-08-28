package com.trajan.negentropy.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tenets")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@SequenceGenerator(name = "tenet_seq", sequenceName = "tenet_seq")
public class TenetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tenet_seq")
    private Long id;
    private String body;

    @Override
    public String toString() {
        return body;
    }
}
