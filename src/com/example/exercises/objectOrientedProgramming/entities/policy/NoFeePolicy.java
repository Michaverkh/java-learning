package com.example.exercises.objectOrientedProgramming.entities.policy;

import com.example.exercises.objectOrientedProgramming.entities.Money;

import java.math.BigDecimal;

public class NoFeePolicy implements FeePolicy {
    @Override
    public Money calculateFor(Money amount) {
        return new Money(amount.getCurrency(), BigDecimal.ZERO);
    }
}
