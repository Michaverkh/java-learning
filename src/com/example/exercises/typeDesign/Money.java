package com.example.exercises.typeDesign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(
                currency,
                "currency must not be null"
        );

        Objects.requireNonNull(amount, "amount must not be null");

        amount = amount.setScale(2, RoundingMode.HALF_UP);

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "amount must be positive"
            );
        }
    }

    public static Money rubles(String amount) {
        return new Money(new BigDecimal(amount), Currency.RUB);
    }
}
