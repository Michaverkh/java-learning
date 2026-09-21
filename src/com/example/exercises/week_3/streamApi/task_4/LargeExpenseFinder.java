package com.example.exercises.week_3.streamApi.task_4;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Точка входа для задания 4. Реализуй поиск согласно условию в учебном материале.
 */
public final class LargeExpenseFinder {
    public static final BigDecimal LARGE_EXPENSE_THRESHOLD = new BigDecimal("200.00");

    private static boolean isLargeExpense(LedgerEntry entry) {
        boolean isNegative = entry.amount().signum() < 0;
        boolean isAmountModuleSatisfyLimit = entry.amount().abs()
                .compareTo(LARGE_EXPENSE_THRESHOLD) >= 0;

        return isNegative && isAmountModuleSatisfyLimit;
    }

    public List<UUID> findIds(List<LedgerEntry> entries) {
        Objects.requireNonNull(entries, "entries should be defined");

        return entries.stream()
                .filter(LargeExpenseFinder::isLargeExpense)
                .map(LedgerEntry::id)
                .toList();
    }

    public boolean hasAny(List<LedgerEntry> entries) {
        Objects.requireNonNull(entries, "entries should be defined");
        
        return entries.stream()
                .anyMatch(LargeExpenseFinder::isLargeExpense);
    }
}
