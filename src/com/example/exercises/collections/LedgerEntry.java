package com.example.exercises.collections;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        AccountId accountId,
        UUID operationId,
        OperationType operationType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(operationType, "operationType must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(balanceAfter, "balanceAfter must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
