package com.example.exercises.week_3.lambdas;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Account;
import com.example.exercises.week_2.objectOrientedProgramming.entities.Money;

public record TransferContext(
        Account sourceAccount,
        Account targetAccount,
        Money amount
) {
}