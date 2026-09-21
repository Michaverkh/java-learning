package com.example.exercises.week_3.streamApi.task_1;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class LedgerHistory {
    private final List<LedgerEntry> entries;
    private final Comparator<LedgerEntry> newestFirst = Comparator
            .comparing(LedgerEntry::createdAt)
            .thenComparing(LedgerEntry::id)
            .reversed(); // и время, и id по убыванию — выбранное правило разрешения равенства

    public LedgerHistory(List<LedgerEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    /**
     * Возвращает страницу истории счёта за интервал [from, to).
     * Порядок: createdAt по убыванию, затем id по убыванию.
     * Если подходящих записей нет или страница за пределами истории, результат — пустой список.
     *
     * @param accountId     обязательный идентификатор счёта
     * @param operationType тип операции; null означает все типы
     * @param from          обязательная нижняя граница времени, включительно
     * @param to            обязательная верхняя граница времени, исключительно
     * @param pageNumber    номер страницы, начиная с 0
     * @param pageSize      размер страницы от 1 до 100 включительно
     * @throws IllegalArgumentException если pageNumber отрицателен или pageSize вне диапазона 1–100
     */
    public List<LedgerEntry> getHistory(
            AccountId accountId,
            OperationType operationType,
            Instant from,
            Instant to,
            int pageNumber,
            int pageSize
    ) {
        Objects.requireNonNull(accountId, "accountId required");
        Objects.requireNonNull(from, "from required");
        Objects.requireNonNull(to, "to required");

        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must be non-negative");
        }

        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageSize should be in limits of [1, 100]");
        }

        return entries.stream()
                .filter(entry -> matchesFilters(entry, accountId, operationType, from, to))
                .sorted(newestFirst)
                .skip((long) pageNumber * pageSize)
                .limit(pageSize)
                .toList();
    }

    private boolean matchesFilters(
            LedgerEntry entry,
            AccountId accountId,
            OperationType operationType,
            Instant from,
            Instant to
    ) {
        boolean isAccountIdEqual = entry.accountId().equals(accountId);
        boolean isOperationTypeConditionPassed = operationType == null || entry.operationType() == operationType;
        boolean isFromConditionPassed = !entry.createdAt().isBefore(from);
        boolean isToConditionPassed = entry.createdAt().isBefore(to);

        return isAccountIdEqual && isOperationTypeConditionPassed && isFromConditionPassed && isToConditionPassed;
    }
}
