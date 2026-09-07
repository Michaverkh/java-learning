package com.example.exercises.collections;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        OutboxEventType type,
        UUID operationId,
        Instant occurredAt
) {
    public OutboxEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}