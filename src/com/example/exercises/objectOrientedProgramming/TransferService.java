package com.example.exercises.objectOrientedProgramming;

import com.example.exercises.objectOrientedProgramming.entities.Money;
import com.example.exercises.objectOrientedProgramming.entities.policy.FeePolicy;

public final class TransferService {
    private final FeePolicy feePolicy;

    public TransferService(FeePolicy feePolicy) {
        this.feePolicy = feePolicy;
    }

    public Money calculateFee(Money amount) {
        return feePolicy.calculateFor(amount);
    }
}
