package com.example.exercises.week_3.lambdas;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Account;
import com.example.exercises.week_2.objectOrientedProgramming.entities.Currency;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class AccountAccessRule {
    private final Predicate<Account> isActive = Account::getIsActive;
    private final Predicate<Account> hasRubCurrency = (account -> account.getBalance().getCurrency() == Currency.RUB);
    AtomicInteger callCount = new AtomicInteger();
    Predicate<Account> hasPositiveBalance = account -> {
        callCount.incrementAndGet();
        return account.getBalance().getAmount().signum() > 0;
    };

    Predicate<Account> canDebit = isActive
            .and(hasRubCurrency)
            .and(hasPositiveBalance);
}
