package com.example.exercises.week_2.objectOrientedProgramming.entities.policy;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;

import java.math.BigDecimal;

public class NoFeePolicy implements FeePolicy {
    @Override
    public Money calculateFor(Money amount) {
        return new Money(amount.getCurrency(), BigDecimal.ZERO);
    }
}
