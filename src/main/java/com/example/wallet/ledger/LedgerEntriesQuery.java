package com.example.wallet.ledger;

import java.time.Instant;

public record LedgerEntriesQuery(
        int page,
        int size,
        OperationType operationType,
        Instant dateFrom,
        Instant dateTo
) {
}