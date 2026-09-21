package com.example.exercises.week_3.lambdas.task_4;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public final record CreateTransferCommand(
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        Currency currency,
        String description) {
}
