package com.example.exercises.typeDesign.optionalBoundary;

import com.example.exercises.objectOrientedProgramming.entities.Account;
import com.example.exercises.objectOrientedProgramming.entities.AccountId;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
}
