package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLedgerRepositoryTest {
    private final InMemoryLedgerRepository repository = new InMemoryLedgerRepository();
    private final AccountId accountId = new AccountId(new UUID(0, 1));
    private final Instant start = Instant.parse("2026-10-01T00:00:00Z");

    private LedgerEntry save(int id, AccountId account, OperationType type, Instant time) {
        boolean incoming = type == OperationType.DEPOSIT || type == OperationType.TRANSFER_IN;
        LedgerEntry entry = new LedgerEntry(new UUID(0, id), account, new UUID(0, 1000 + id), type,
                new BigDecimal(incoming ? "1.00" : "-1.00"), new BigDecimal("100.00"), time);
        repository.save(entry);
        return entry;
    }

    @Test
    void dateRangeIncludesStartExcludesEndAndFiltersAccountAndType() {
        save(1, accountId, OperationType.DEPOSIT, start.minusSeconds(1));
        LedgerEntry atStart = save(2, accountId, OperationType.DEPOSIT, start);
        LedgerEntry inside = save(3, accountId, OperationType.DEPOSIT, start.plusSeconds(1));
        save(4, accountId, OperationType.DEPOSIT, start.plusSeconds(2));
        save(5, accountId, OperationType.WITHDRAWAL, start);
        save(6, new AccountId(new UUID(0, 2)), OperationType.DEPOSIT, start);

        var result = repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 1, OperationType.DEPOSIT,
                start, start.plusSeconds(2)));
        assertEquals(List.of(inside), result.items());
        assertEquals(2, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(List.of(atStart), repository.findAllWithFilters(accountId, new LedgerEntriesQuery(1, 1,
                OperationType.DEPOSIT, start, start.plusSeconds(2))).items());
    }

    @Test
    void datesCanBeOmittedIndependently() {
        LedgerEntry before = save(1, accountId, OperationType.DEPOSIT, start.minusSeconds(1));
        LedgerEntry atStart = save(2, accountId, OperationType.DEPOSIT, start);
        assertEquals(List.of(atStart), repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20,
                null, start, null)).items());
        assertEquals(List.of(before), repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20,
                null, null, start)).items());
        assertEquals(List.of(atStart, before), repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20,
                null, null, null)).items());
    }

    @Test
    void pagesPreserveTimeAndIdOrderWithoutOverlap() {
        // Обратный порядок добавления и одинаковые даты проверяют сортировку по id.
        for (int id = 25; id >= 1; id--) {
            save(id, accountId, OperationType.DEPOSIT, start.plusSeconds(id / 2));
        }
        var first = repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20, null, null, null));
        var second = repository.findAllWithFilters(accountId, new LedgerEntriesQuery(1, 20, null, null, null));
        List<UUID> expectedIds = IntStream.rangeClosed(1, 25).boxed()
                .sorted(java.util.Comparator.<Integer>comparingInt(id -> id / 2).reversed()
                        .thenComparing(java.util.Comparator.reverseOrder()))
                .map(id -> new UUID(0, id)).toList();
        assertEquals(expectedIds.subList(0, 20), first.items().stream().map(LedgerEntry::id).toList());
        assertEquals(expectedIds.subList(20, 25), second.items().stream().map(LedgerEntry::id).toList());
        assertEquals(25, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(25, second.totalElements());
        assertEquals(2, second.totalPages());
        var outside = repository.findAllWithFilters(accountId, new LedgerEntriesQuery(Integer.MAX_VALUE, 20, null, null, null));
        assertTrue(outside.items().isEmpty());
        assertEquals(25, outside.totalElements());
        assertEquals(2, outside.totalPages());
    }

    @ParameterizedTest
    @CsvSource({"0,20,0", "1,20,1", "20,20,1", "21,20,2", "40,20,2", "41,20,3"})
    void totalPagesRoundsUp(int count, int size, long expectedPages) {
        for (int id = 1; id <= count; id++) {
            save(id, accountId, OperationType.DEPOSIT, start);
        }
        var result = repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, size, null, null, null));
        assertEquals(count, result.totalElements());
        assertEquals(expectedPages, result.totalPages());
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void invalidPaginationIsRejected(int page, int size) {
        assertThrows(IllegalArgumentException.class,
                () -> repository.findAllWithFilters(accountId, new LedgerEntriesQuery(page, size, null, null, null)));
    }

    @Test
    void emptyOrReversedDateRangeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20, null, start, start)));
        assertThrows(IllegalArgumentException.class,
                () -> repository.findAllWithFilters(accountId, new LedgerEntriesQuery(0, 20, null, start, start.minusSeconds(1))));
    }
}
