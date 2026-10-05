package com.example.wallet.accounts;

import com.example.wallet.clients.ClientId;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;

import java.math.BigDecimal;
import java.util.Objects;

public final class Account {
    private final AccountId accountId;
    private final ClientId clientId;
    private final Currency currency;
    private boolean isActive;

    private BigDecimal balance;

    public Account(AccountId accountId, ClientId clientId, Money initialBalance) {
        this.accountId = Objects.requireNonNull(
                accountId,
                "accountId must not be null"
        );

        Objects.requireNonNull(
                initialBalance,
                "initialBalance must not be null"
        );
        this.clientId = Objects.requireNonNull(
                clientId,
                "clientId must not be null"
        );

        this.currency = initialBalance.getCurrency();
        this.balance = initialBalance.getAmount();
        isActive = true;
    }

    public void deposit(Money money) {
        requireSameCurrency(money);
        requirePositive(money);

        balance = balance.add(money.getAmount());
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

    public AccountId id() {
        return this.accountId;
    }

    public ClientId clientId() {
        return this.clientId;
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
