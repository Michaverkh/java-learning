package com.example.exercises.week_2.objectOrientedProgramming.entities.policy;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;

import java.math.BigDecimal;
import java.util.Objects;

public class FixedFeePolicy implements FeePolicy {
    final BigDecimal rate;

    public FixedFeePolicy(BigDecimal rate) {
        Objects.requireNonNull(rate);
        this.rate = rate;
    }

    @Override
    public Money calculateFor(Money amount) {
        return new Money(amount.getCurrency(), rate);
    }
}
