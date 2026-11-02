package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.shared.pagination.PageResponse;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public class InMemoryLedgerRepository implements LedgerRepository {
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

    public PageResponse<LedgerEntry> findAllWithFilters(AccountId accountId, LedgerEntriesQuery ledgerEntriesQuery) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(ledgerEntriesQuery, "ledgerEntriesQuery must not be null");

        int page = ledgerEntriesQuery.page();
        int size = ledgerEntriesQuery.size();
        OperationType operationType = ledgerEntriesQuery.operationType();
        Instant dateFrom = ledgerEntriesQuery.dateFrom();
        Instant dateTo = ledgerEntriesQuery.dateTo();

        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (dateFrom != null && dateTo != null && !dateFrom.isBefore(dateTo)) {
            throw new IllegalArgumentException("dateFrom must be before dateTo");
        }
        List<LedgerEntry> ledgerEntries = entries.values().stream()
                .filter(entry -> entry.accountId().equals(accountId)
                        && (operationType == null || entry.operationType() == operationType)
                        && (dateFrom == null || !entry.createdAt().isBefore(dateFrom))
                        && (dateTo == null || entry.createdAt().isBefore(dateTo)))
                .sorted(Comparator.comparing(LedgerEntry::createdAt).reversed()
                        .thenComparing(LedgerEntry::id, Comparator.reverseOrder()))
                .toList();

        long totalElements = ledgerEntries.size();
        long totalPages = Math.ceilDiv(totalElements, size);

        // long предотвращает переполнение при большом номере страницы.
        long offset = (long) page * size;
        List<LedgerEntry> items = ledgerEntries.stream()
                .skip(offset)
                .limit(size)
                .toList();

        return new PageResponse<>(items, page, size, totalElements, totalPages);
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
