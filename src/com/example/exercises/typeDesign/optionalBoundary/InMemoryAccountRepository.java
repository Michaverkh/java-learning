package com.example.exercises.typeDesign.optionalBoundary;

import com.example.exercises.objectOrientedProgramming.entities.Account;
import com.example.exercises.objectOrientedProgramming.entities.AccountId;
import com.example.exercises.objectOrientedProgramming.entities.Money;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<AccountId, Account> accounts = new HashMap<>();

    public InMemoryAccountRepository() {
        AccountId firstAccountId = new AccountId("account-1");
        AccountId secondAccountId = new AccountId("account-2");
        AccountId thirdAccountId = new AccountId("account-3");

        accounts.put(
                firstAccountId,
                new Account(firstAccountId, Money.rubles("1000.00"))
        );
        accounts.put(
                secondAccountId,
                new Account(secondAccountId, Money.rubles("2500.00"))
        );
        accounts.put(
                thirdAccountId,
                new Account(thirdAccountId, Money.rubles("10000.00"))
        );
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }
}
