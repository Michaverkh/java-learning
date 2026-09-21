package com.example.exercises.week_3.streamApi.task_2;

import java.util.Objects;
import java.util.UUID;

/** Идентификатор счёта в упражнении об оборотах. */
public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
