package com.example.exercises.objectOrientedProgramming.entities.policy;

import com.example.exercises.objectOrientedProgramming.entities.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class PercentageFeePolicy implements FeePolicy {
    final BigDecimal rate;

    public PercentageFeePolicy(BigDecimal rate) {
        Objects.requireNonNull(rate);
        this.rate = rate;
    }

    @Override
    public Money calculateFor(Money amount) {
        BigDecimal feeAmount = amount.getAmount()
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new Money(amount.getCurrency(), feeAmount);
    }
}
