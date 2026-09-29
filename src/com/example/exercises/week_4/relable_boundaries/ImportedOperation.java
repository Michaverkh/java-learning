package com.example.exercises.week_4.relable_boundaries;

import java.time.Instant;
import java.util.Objects;

/** Корректно разобранная строка CSV. */
public record ImportedOperation(
        OperationId id,
        Instant occurredAt,
        Money money,
        OperationType type
) {
    public ImportedOperation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(type, "type must not be null");
    }
}
