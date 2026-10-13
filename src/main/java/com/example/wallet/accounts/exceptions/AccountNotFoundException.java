package com.example.wallet.accounts.exceptions;

import com.example.wallet.accounts.AccountId;

import java.util.Objects;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(AccountId accountId) {
        super("Счет не найден: " + Objects.requireNonNull(accountId, "accountId must not be null").getValue());
    }
}
