package com.example.exercises.week_2.typeDesign.optionalBoundary;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Account;
import com.example.exercises.week_2.objectOrientedProgramming.entities.AccountId;

public final class AccountLookupService {
    private final AccountRepository repository;

    public AccountLookupService(AccountRepository repository) {
        this.repository = repository;
    }

    public Account getRequired(AccountId id) {
        return repository.findById(id).orElseThrow(AccountNotFoundException::new);
    }
}
