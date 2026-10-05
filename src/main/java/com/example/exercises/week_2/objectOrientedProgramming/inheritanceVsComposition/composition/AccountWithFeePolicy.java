package com.example.exercises.week_2.objectOrientedProgramming.inheritanceVsComposition.composition;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;
import com.example.exercises.week_2.objectOrientedProgramming.entities.policy.FeePolicy;

import java.util.Objects;

public class AccountWithFeePolicy {
    private final FeePolicy feePolicy;

    public AccountWithFeePolicy(FeePolicy feePolicy) {
        this.feePolicy = Objects.requireNonNull(
                feePolicy,
                "feePolicy must not be null"
        );
    }

    public Money calculateFee(Money amount) {
        return feePolicy.calculateFor(amount);
    }
}
