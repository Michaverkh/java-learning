package com.example.wallet.accounts;

import java.util.Objects;
import java.util.UUID;

public final class AccountId {
    private final UUID accountId;

    public AccountId(String accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        this.accountId = UUID.fromString(accountId);
    }

    public UUID getValue() {
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
