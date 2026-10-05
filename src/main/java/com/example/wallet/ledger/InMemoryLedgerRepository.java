package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public final class InMemoryLedgerRepository implements LedgerRepository {
    private final Map<UUID, LedgerEntry> entries = new HashMap<>();

    @Override
    public void save(LedgerEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        if (entries.putIfAbsent(entry.id(), entry) != null) {
            throw new IllegalStateException("ledger entry with this id already exists");
        }
    }

    @Override
    public Optional<LedgerEntry> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<LedgerEntry> findAllByAccountId(AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return entries.values().stream()
                .filter(entry -> entry.accountId().equals(accountId))
                .sorted(Comparator.comparing(LedgerEntry::createdAt).reversed()
                        .thenComparing(LedgerEntry::id))
                .toList();
    }

    @Override
    public List<LedgerEntry> findAllByOperationId(UUID operationId) {
        Objects.requireNonNull(operationId, "operationId must not be null");
        return entries.values().stream()
                .filter(entry -> entry.operationId().equals(operationId))
                .sorted(Comparator.comparing(LedgerEntry::createdAt)
                        .thenComparing(LedgerEntry::id))
                .toList();
    }
}
