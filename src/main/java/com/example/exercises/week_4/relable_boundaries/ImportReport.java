package com.example.exercises.week_4.relable_boundaries;

import java.util.List;
import java.util.Objects;

/** Неизменяемый итог одной попытки импорта. */
public record ImportReport(
        List<ImportedOperation> operations,
        List<ImportIssue> issues
) {
    public ImportReport {
        Objects.requireNonNull(operations, "operations must not be null");
        Objects.requireNonNull(issues, "issues must not be null");

        operations = List.copyOf(operations);
        issues = List.copyOf(issues);
    }
}
