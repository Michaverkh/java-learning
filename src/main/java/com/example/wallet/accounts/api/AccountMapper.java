package com.example.wallet.accounts.api;

import com.example.wallet.accounts.Account;
import com.example.wallet.shared.money.Money;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class AccountMapper {
    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        Objects.requireNonNull(account, "account must not be null");
        Money balance = account.getBalance();

        return new AccountResponse(
                account.id().getValue(),
                account.clientId().value(),
                balance.getCurrency().name(),
                balance.getAmount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                account.status().name(),
                account.createdAt()
        );
    }

    public static List<AccountResponse> toListResponse(List<Account> accounts) {
        List<AccountResponse> result = accounts.stream().map(AccountMapper::toResponse).toList();

        return List.copyOf(result);
    }
}
