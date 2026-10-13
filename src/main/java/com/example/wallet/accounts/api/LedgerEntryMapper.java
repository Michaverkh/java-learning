package com.example.wallet.accounts.api;

import com.example.wallet.ledger.LedgerEntry;

import java.math.RoundingMode;
import java.util.Objects;

public final class LedgerEntryMapper {
    private LedgerEntryMapper() {
    }

    public static LedgerEntryResponse toResponse(LedgerEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");

        return new LedgerEntryResponse(
                entry.id(),
                entry.accountId().getValue(),
                entry.operationId(),
                entry.operationType().name(),
                entry.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                entry.balanceAfter().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                entry.createdAt()
        );
    }
}
