package com.example.exercises.objectOrientedProgramming.inheritanceVsComposition.composition;

import com.example.exercises.objectOrientedProgramming.entities.Money;
import com.example.exercises.objectOrientedProgramming.entities.policy.FeePolicy;

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
