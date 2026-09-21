package com.example.exercises.week_2.typeDesign.optionalBoundary;

import com.example.exercises.week_2.objectOrientedProgramming.entities.Account;
import com.example.exercises.week_2.objectOrientedProgramming.entities.AccountId;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(AccountId id);
}
