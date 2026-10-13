package com.example.wallet.accounts.exceptions;

import com.example.wallet.accounts.AccountId;

import java.util.Objects;

public class AccountNotAllowCloseException extends RuntimeException {
    public AccountNotAllowCloseException(AccountId accountId) {
        super("Нельзя закрыть аккаунт с не нулевым балансом: " + Objects.requireNonNull(accountId, "accountId must not be null").getValue());
    }
}
