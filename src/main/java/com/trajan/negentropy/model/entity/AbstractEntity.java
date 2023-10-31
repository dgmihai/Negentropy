package com.trajan.negentropy.model.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@MappedSuperclass
@NoArgsConstructor
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
        return id() != null ? id().hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return "AbstractEntity[" + id() + "]";
    }
}
