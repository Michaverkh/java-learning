package com.example.exercises.week_2.typeDesign.genericRepository;

import com.example.exercises.week_2.typeDesign.AccountId;

import java.util.Objects;

public record Account(AccountId id) {
    public Account {
        Objects.requireNonNull(
                id,
                "id must not be null"
        );
    }

    public AccountId getId() {
        return id;
    }
}
