package com.example.exercises.collections;

import java.util.Objects;
import java.util.UUID;

public record AccountId(UUID value) implements Comparable<AccountId> {
    public AccountId {
        Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public int compareTo(AccountId other) {
        return value.compareTo(other.value);
    }
}
