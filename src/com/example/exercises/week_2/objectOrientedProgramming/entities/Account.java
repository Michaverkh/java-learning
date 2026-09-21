package com.example.exercises.week_2.objectOrientedProgramming.entities;

import java.math.BigDecimal;
import java.util.Objects;

public final class Account {
    private final AccountId accountId;
    private final Currency currency;
    private boolean isActive;

    // Именно Account владеет изменяемым балансом.
    private BigDecimal balance;

    public Account(AccountId accountId, Money initialBalance) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "accountId must not be null"
        );

        Objects.requireNonNull(
                initialBalance,
                "initialBalance must not be null"
        );

        this.currency = initialBalance.getCurrency();
        this.balance = initialBalance.getAmount();
        this.isActive = false;
    }

    public void deposit(Money money) {
        requireSameCurrency(money);
        requirePositive(money);

        balance = balance.add(money.getAmount());
        isActive = true;
    }

    public void withdraw(Money money) {
        requireSameCurrency(money);
        requirePositive(money);

        if (balance.compareTo(money.getAmount()) < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        balance = balance.subtract(money.getAmount());
    }

    public boolean getIsActive() {
        return this.isActive;
    }

    public AccountId getId() {
        return this.accountId;
    }

    public Money getBalance() {
        return new Money(currency, balance);
    }

    private void requireSameCurrency(Money money) {
        Objects.requireNonNull(money, "money must not be null");

        if (currency != money.getCurrency()) {
            throw new IllegalArgumentException(
                    "currency must be " + currency
            );
        }
    }

    private void requirePositive(Money money) {
        if (money.getAmount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "amount must be positive"
            );
        }
    }
}