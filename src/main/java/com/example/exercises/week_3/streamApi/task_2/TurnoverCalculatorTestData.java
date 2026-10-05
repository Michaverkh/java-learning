package com.example.exercises.week_3.streamApi.task_2;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Воспроизводимые данные из условия задания 2. */
public record TurnoverCalculatorTestData(
        AccountId accountA,
        AccountId accountB,
        AccountId accountC,
        List<AccountId> accountIds,
        List<LedgerEntry> entries
) {
    public static TurnoverCalculatorTestData create() {
        AccountId accountA = new AccountId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
        AccountId accountB = new AccountId(UUID.fromString("00000000-0000-0000-0000-00000000000b"));
        AccountId accountC = new AccountId(UUID.fromString("00000000-0000-0000-0000-00000000000c"));

        UUID depositId = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
        UUID transferId = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
        UUID withdrawalId = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

        Instant firstMoment = Instant.parse("2026-01-01T10:00:00Z");
        Instant secondMoment = Instant.parse("2026-01-01T11:00:00Z");
        Instant thirdMoment = Instant.parse("2026-01-01T12:00:00Z");

        List<LedgerEntry> entries = List.of(
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"), accountA, depositId,
                        OperationType.DEPOSIT, new BigDecimal("1000.00"), new BigDecimal("1000.00"), firstMoment
                ),
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000002"), accountA, transferId,
                        OperationType.TRANSFER_OUT, new BigDecimal("-250.00"), new BigDecimal("750.00"), secondMoment
                ),
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000003"), accountB, transferId,
                        OperationType.TRANSFER_IN, new BigDecimal("250.00"), new BigDecimal("250.00"), secondMoment
                ),
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000004"), accountA, withdrawalId,
                        OperationType.WITHDRAWAL, new BigDecimal("-100.00"), new BigDecimal("650.00"), thirdMoment
                )
        );

        return new TurnoverCalculatorTestData(accountA, accountB, accountC, List.of(accountA, accountB, accountC), entries);
    }
}
