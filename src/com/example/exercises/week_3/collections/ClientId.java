package com.example.exercises.week_3.collections;

import java.util.Objects;
import java.util.UUID;

public record ClientId(UUID value) {
    public ClientId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
