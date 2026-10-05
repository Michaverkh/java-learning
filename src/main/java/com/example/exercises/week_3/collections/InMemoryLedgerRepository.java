package com.example.exercises.week_3.collections;

import java.util.*;

public final class InMemoryLedgerRepository {
    private final Map<AccountId, List<LedgerEntry>> entriesByAccount =
            new HashMap<>();

    public void save(LedgerEntry entry) {
        entriesByAccount.computeIfAbsent(entry.accountId(), ignored -> new ArrayList<>()).add(entry);
    }

    public List<LedgerEntry> findAllByAccountId(AccountId accountId) {
        List<LedgerEntry> entriesByAccountId = entriesByAccount.getOrDefault(accountId, List.of());
        List<LedgerEntry> copyOfEntriesByAccountId = new ArrayList<>(entriesByAccountId);

        Comparator<LedgerEntry> historyOrder = Comparator.comparing(LedgerEntry::createdAt).reversed().thenComparing(LedgerEntry::id);
        copyOfEntriesByAccountId.sort(historyOrder);

        return List.copyOf(copyOfEntriesByAccountId);
    }

    public Set<OperationType> findOperationTypes(AccountId accountId) {
        Set<OperationType> operationTypes = EnumSet.noneOf(OperationType.class);

        List<LedgerEntry> entriesByAccountId = entriesByAccount.getOrDefault(accountId, List.of());

        for (LedgerEntry entry : entriesByAccountId) {
            operationTypes.add(entry.operationType());
        }

        return Set.copyOf(operationTypes);
    }
}