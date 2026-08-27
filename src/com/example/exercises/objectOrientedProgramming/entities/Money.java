package com.example.exercises.objectOrientedProgramming.entities;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money {
    private final Currency currency;
    private final BigDecimal amount;

    public Money(Currency currency, BigDecimal amount) {
        this.currency = Objects.requireNonNull(
                currency,
                "currency must not be null"
        );

        Objects.requireNonNull(amount, "amount must not be null");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "amount must be non-negative"
            );
        }

        this.amount = amount;
    }

    public static Money rubles(String amount) {
        return new Money(Currency.RUB, new BigDecimal(amount));
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}