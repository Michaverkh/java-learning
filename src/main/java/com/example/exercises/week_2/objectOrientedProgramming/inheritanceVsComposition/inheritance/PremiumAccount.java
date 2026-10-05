package com.example.exercises.week_2.objectOrientedProgramming.inheritanceVsComposition.inheritance;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;
import com.example.exercises.week_2.objectOrientedProgramming.entities.policy.NoFeePolicy;

public class PremiumAccount extends AccountWithFee {
    @Override
    public Money calculateFee(Money amount) {
        return new NoFeePolicy().calculateFor(amount);
    }
}
