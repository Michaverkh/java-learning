package com.example.exercises.week_3.lambdas;

import com.example.exercises.week_3.collections.AccountId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        AccountId accountId,
        BigDecimal amount
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }
}
