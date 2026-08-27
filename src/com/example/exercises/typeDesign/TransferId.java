package com.example.exercises.typeDesign;

import java.util.Objects;
import java.util.UUID;

public record TransferId(UUID value) {
    public TransferId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
