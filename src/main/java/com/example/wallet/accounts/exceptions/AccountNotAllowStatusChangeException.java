package com.example.wallet.accounts.exceptions;

import com.example.wallet.accounts.AccountId;

import java.util.Objects;

public class AccountNotAllowStatusChangeException extends RuntimeException {
    public AccountNotAllowStatusChangeException(AccountId accountId) {
        super("Изменение статуса закрытого счёта запрещено: "
                + Objects.requireNonNull(accountId, "accountId must not be null").getValue());
    }
}
