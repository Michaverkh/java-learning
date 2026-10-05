package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемая запись об изменении баланса. Валюта определяется счётом. */
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

        if (amount.scale() > 2 || balanceAfter.scale() > 2) {
            throw new IllegalArgumentException("amount and balance must have at most two decimal places");
        }

        boolean incoming = operationType == OperationType.DEPOSIT
                || operationType == OperationType.TRANSFER_IN;
        if ((incoming && amount.signum() <= 0) || (!incoming && amount.signum() >= 0)) {
            throw new IllegalArgumentException("amount sign must match operation type and must not be zero");
        }

        if (balanceAfter.signum() < 0) {
            throw new IllegalArgumentException("balanceAfter must be non-negative");
        }
    }
}
