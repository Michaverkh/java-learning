package com.example.exercises.typeDesign.genericRepository;

import com.example.exercises.typeDesign.AccountId;

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
