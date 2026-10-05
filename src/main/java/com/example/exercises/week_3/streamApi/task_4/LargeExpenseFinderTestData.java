package com.example.exercises.week_3.streamApi.task_4;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Воспроизводимые данные для самопроверки задания 4. */
public record LargeExpenseFinderTestData(
        List<LedgerEntry> entries,
        UUID largeExpenseId
) {
    public static LargeExpenseFinderTestData create() {
        AccountId accountA = new AccountId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
        AccountId accountB = new AccountId(UUID.fromString("00000000-0000-0000-0000-00000000000b"));

        UUID depositId = UUID.fromString("00000000-0000-0000-0000-0000000000d1");
        UUID transferId = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
        UUID withdrawalId = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
        UUID transferOutEntryId = UUID.fromString("10000000-0000-0000-0000-000000000002");

        List<LedgerEntry> entries = List.of(
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000001"), accountA, depositId,
                        OperationType.DEPOSIT, new BigDecimal("1000.00")
                ),
                new LedgerEntry(
                        transferOutEntryId, accountA, transferId,
                        OperationType.TRANSFER_OUT, new BigDecimal("-250.00")
                ),
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000003"), accountB, transferId,
                        OperationType.TRANSFER_IN, new BigDecimal("250.00")
                ),
                new LedgerEntry(
                        UUID.fromString("10000000-0000-0000-0000-000000000004"), accountA, withdrawalId,
                        OperationType.WITHDRAWAL, new BigDecimal("-100.00")
                )
        );

        return new LargeExpenseFinderTestData(entries, transferOutEntryId);
    }
}
