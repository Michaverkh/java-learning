package com.example.exercises.week_4.relable_boundaries;

import java.util.Objects;
import java.util.UUID;

/** Неизменяемый идентификатор импортированной денежной операции. */
public record OperationId(UUID value) {
    public OperationId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
