package com.example.exercises.week_3.streamApi.task_4;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Неизменяемая запись об изменении баланса.
 * Положительный amount означает поступление, отрицательный — расход.
 */
public record LedgerEntry(
        UUID id,
        AccountId accountId,
        UUID operationId,
        OperationType operationType,
        BigDecimal amount
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
