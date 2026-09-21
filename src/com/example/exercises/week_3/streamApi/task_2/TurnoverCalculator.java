package com.example.exercises.week_3.streamApi.task_2;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Точка входа для задания 2. Реализуй расчёт согласно условию в учебном материале.
 */
public final class TurnoverCalculator {

    public Map<AccountId, AccountTurnover> calculate(
            List<AccountId> accountIds,
            List<LedgerEntry> entries
    ) {
        Objects.requireNonNull(accountIds, "accountIds must not be null");
        Objects.requireNonNull(entries, "entries must not be null");

        Map<AccountId, AccountTurnover> turnoversByAccount = entries.stream()
                .filter(entry -> belongsToRequestedAccounts(entry, accountIds))
                .collect(Collectors.groupingBy(
                        LedgerEntry::accountId,
                        Collectors.reducing(
                                AccountTurnover.zero(),
                                this::toAccountTurnover,
                                AccountTurnover::add
                        )
                ));

        return accountIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        accountId -> turnoversByAccount.getOrDefault(accountId, AccountTurnover.zero()),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private boolean belongsToRequestedAccounts(LedgerEntry entry, List<AccountId> accountIds) {
        return accountIds.stream()
                .anyMatch(accountId -> accountId.equals(entry.accountId()));
    }

    private AccountTurnover toAccountTurnover(LedgerEntry ledgerEntry) {
        BigDecimal amount = ledgerEntry.amount();
        BigDecimal zero = new BigDecimal("0.00");

        if (amount.signum() > 0) {
            return new AccountTurnover(amount, zero, amount);
        }

        if (amount.signum() < 0) {
            return new AccountTurnover(zero, amount.abs(), amount);
        }

        return AccountTurnover.zero();
    }

}
