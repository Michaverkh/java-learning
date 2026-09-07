package com.example.exercises.collections;

import java.util.Objects;
import java.util.UUID;

public record ClientId(UUID value) {
    public ClientId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
