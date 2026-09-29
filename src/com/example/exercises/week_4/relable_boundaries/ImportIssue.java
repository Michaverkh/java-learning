package com.example.exercises.week_4.relable_boundaries;

import java.util.Objects;

/** Описание ошибки одной строки CSV без прекращения всего импорта. */
public record ImportIssue(int lineNumber, String field, String message) {
    public ImportIssue {
        if (lineNumber < 1) {
            throw new IllegalArgumentException("lineNumber must be positive");
        }

        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
