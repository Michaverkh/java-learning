package com.example.exercises.premitivesAndReferenceTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class Money {
    private final BigDecimal amount;
    private final Currency currency;


    public Money(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");

        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Money addMoney(Money money) {
        if (this.currency != money.getCurrency()) {
            throw new IllegalArgumentException("currencies must match");
        }

        return new Money(this.amount.add(money.getAmount()), this.currency);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Money other)) {
            return false;
        }
        return amount.equals(other.amount)
                && currency == other.currency;
    }

    public enum Currency {
        EUR,
        RUB,
        USD
    }
}
