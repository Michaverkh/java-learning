package com.example.exercises.objectOrientedProgramming.entities.policy;

import com.example.exercises.objectOrientedProgramming.entities.Money;

public interface FeePolicy {
    Money calculateFor(Money amount);
}
