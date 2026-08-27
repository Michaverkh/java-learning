package com.example.exercises.objectOrientedProgramming.inheritanceVsComposition.inheritance;

import com.example.exercises.objectOrientedProgramming.entities.Money;
import com.example.exercises.objectOrientedProgramming.entities.policy.NoFeePolicy;

public class PremiumAccount extends AccountWithFee {
    @Override
    public Money calculateFee(Money amount) {
        return new NoFeePolicy().calculateFor(amount);
    }
}
