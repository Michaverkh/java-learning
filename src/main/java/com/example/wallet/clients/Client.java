package com.example.wallet.clients;

import java.time.Instant;
import java.util.Objects;

public record Client(
        ClientId id,
        String externalId,
        String name,
        ClientStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public Client {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        if (externalId.isBlank()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }
    }
}
