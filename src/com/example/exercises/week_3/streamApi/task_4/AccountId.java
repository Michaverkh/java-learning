package com.example.exercises.week_3.streamApi.task_4;

import java.util.Objects;
import java.util.UUID;

/** Идентификатор кошелька в упражнении о крупных расходах. */
public record AccountId(UUID value) {
    public AccountId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
