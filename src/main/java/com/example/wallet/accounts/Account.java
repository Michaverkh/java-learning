package com.example.wallet.accounts;

import com.example.wallet.accounts.exceptions.AccountNotAllowCloseException;
import com.example.wallet.accounts.exceptions.AccountNotAllowStatusChangeException;
import com.example.wallet.clients.ClientId;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Account {
    private final AccountId accountId;
    private final ClientId clientId;
    private final Currency currency;
    private final Instant createdAt;
    private AccountStatus status;
    private BigDecimal balance;

    public Account(AccountId accountId, ClientId clientId, Money initialBalance, Instant createdAt) {
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
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.status = AccountStatus.ACTIVE;
    }

    public void deposit(Money money) {
        requireActive();
        requireSameCurrency(money);
        requirePositive(money);

        balance = balance.add(money.getAmount());
    }

    public void withdraw(Money money) {
        requireActive();
        requireSameCurrency(money);
        requirePositive(money);

        if (balance.compareTo(money.getAmount()) < 0) {
            throw new IllegalStateException("insufficient funds");
        }

        balance = balance.subtract(money.getAmount());
    }

    public AccountStatus status() {
        return this.status;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public void block() {
        requireNotClosed();
        status = AccountStatus.BLOCKED;
    }

    public void unblock() {
        requireNotClosed();
        status = AccountStatus.ACTIVE;
    }

    public void close() {
        if (balance.signum() != 0) {
            throw new AccountNotAllowCloseException(accountId);
        }
        status = AccountStatus.CLOSED;
    }

    public AccountId id() {
        return this.accountId;
    }

    public ClientId clientId() {
        return this.clientId;
    }

    public Instant createdAt() {
        return this.createdAt;
    }

    public Money getBalance() {
        return new Money(currency, balance);
    }

    private void requireActive() {
        if (!isActive()) {
            throw new IllegalStateException("account must be active");
        }
    }

    private void requireNotClosed() {
        if (status == AccountStatus.CLOSED) {
            throw new AccountNotAllowStatusChangeException(accountId);
        }
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
