package com.example.exercises.week_4.relable_boundaries;

import java.math.BigDecimal;
import java.util.Objects;

/** Денежная сумма операции без неявного округления. */
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("amount must have at most two decimal places");
        }
    }
}
