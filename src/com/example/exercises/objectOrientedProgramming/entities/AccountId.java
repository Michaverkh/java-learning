package com.example.exercises.objectOrientedProgramming.entities;

import java.util.Objects;

public final class AccountId {
    private final String accountId;

    public AccountId(String accountId) {
        this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
    }

    public String getValue() {
        return this.accountId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AccountId other)) {
            return false;
        }

        return accountId.equals(other.accountId);
    }

    @Override
    public int hashCode() {
        return accountId.hashCode();
    }
}
