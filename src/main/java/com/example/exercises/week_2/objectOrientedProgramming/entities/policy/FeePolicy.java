package com.example.exercises.week_2.objectOrientedProgramming.entities.policy;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;

public interface FeePolicy {
    Money calculateFor(Money amount);
}
