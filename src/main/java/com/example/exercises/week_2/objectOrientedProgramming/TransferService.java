package com.example.exercises.week_2.objectOrientedProgramming;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;
import com.example.exercises.week_2.objectOrientedProgramming.entities.policy.FeePolicy;

public final class TransferService {
    private final FeePolicy feePolicy;

    public TransferService(FeePolicy feePolicy) {
        this.feePolicy = feePolicy;
    }

    public Money calculateFee(Money amount) {
        return feePolicy.calculateFor(amount);
    }
}
