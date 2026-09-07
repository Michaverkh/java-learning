package com.example.exercises.collections;

import java.util.Objects;

public record Account(AccountId id, ClientId clientId) {
    public Account {
        Objects.requireNonNull(
                id,
                "id must not be null"
        );
        Objects.requireNonNull(
                clientId,
                "clientId must not be null"
        );
    }

    public AccountId getId() {
        return id;
    }
}
