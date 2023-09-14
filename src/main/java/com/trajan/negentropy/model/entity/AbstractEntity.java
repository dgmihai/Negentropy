package com.trajan.negentropy.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

@MappedSuperclass
@NoArgsConstructor
@Accessors(fluent = true)
@Getter
@Setter
public abstract class AbstractEntity {

    public abstract Long id();

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractEntity that)) {
            return false;
        }
        return Objects.equals(id(), that.id());
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public String toString() {
        return "changes: " + id();
    }
}
