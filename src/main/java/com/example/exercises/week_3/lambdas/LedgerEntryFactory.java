package com.example.exercises.week_3.lambdas;

import com.example.exercises.week_3.collections.AccountId;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.BiFunction;

public final class LedgerEntryFactory {
    public final static BiFunction<AccountId, BigDecimal, LedgerEntry> createLedgerEntry = (accountId, amount) -> {
        final LedgerEntry ledgerEntry = new LedgerEntry(UUID.randomUUID(), accountId, amount);
        return ledgerEntry;
    };

}

// реализация через @FunctionalInterface лучше описывает доменную область, т к мы може прокинуть туда все необходимы аргументы, а BiFunction предполагает что мы из двух типов пораждаем следующий
// operationId, createdAt и balanceAfter я пока опустил, но их можно объединить в объект ledgerEntryInitialData
// да, двух аргументов недостаточно


