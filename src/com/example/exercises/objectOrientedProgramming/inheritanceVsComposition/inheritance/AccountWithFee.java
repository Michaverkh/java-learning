package com.example.exercises.objectOrientedProgramming.inheritanceVsComposition.inheritance;

import com.example.exercises.objectOrientedProgramming.entities.Money;
import com.example.exercises.objectOrientedProgramming.entities.policy.PercentageFeePolicy;

import java.math.BigDecimal;

public class AccountWithFee {
    public Money calculateFee(Money amount) {
        return new PercentageFeePolicy(
                new BigDecimal("1")
        ).calculateFor(amount);
    }
}
