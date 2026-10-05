package com.example.exercises.week_3.lambdas.task_4;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transfer(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        Instant createdAt,
        Instant completedAt
) {
}
