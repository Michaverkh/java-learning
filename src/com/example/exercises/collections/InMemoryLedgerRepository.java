package com.example.exercises.collections;

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
    /*
      почему `List` хуже выражает уникальность;
       - потому что в Set уникальность реализована из каробки, а влист перед каждой вставкой нужно проходить по всему list и проверять не содержит ли он уже вставляемое значение.
      почему порядок `HashSet` нельзя показывать клиенту как стабильный контракт;
      - потому что HashSet обеспечивает уникальность, а не порядок. при вставке значения которое уже есть в set, его индекс может измениться
      когда для результата нужен `List`, отсортированный отдельно. - Когда важна история всех опираций.
     */
}