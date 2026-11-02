package com.example.wallet.accounts;

import com.example.wallet.shared.money.Money;

import java.util.Optional;

public interface AccountBalanceReader {
    Optional<Money> findBalanceByAccountId(AccountId accountId);
}
