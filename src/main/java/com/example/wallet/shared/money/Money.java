package com.example.wallet.shared.money;

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

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Money amount must have at most two decimal places"
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
