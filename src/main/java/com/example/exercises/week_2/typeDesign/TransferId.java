package com.example.exercises.week_2.typeDesign;

import java.util.Objects;
import java.util.UUID;

public record TransferId(UUID value) {
    public TransferId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
