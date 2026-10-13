package com.example.wallet.accounts.api;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID accountId,
        UUID operationId,
        String operationType,
        String amount,
        String balanceAfter,
        Instant createdAt
) {
}
