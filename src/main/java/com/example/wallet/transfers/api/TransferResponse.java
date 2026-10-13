package com.example.wallet.transfers.api;

import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        String amount,
        String currency,
        String status,
        Instant createdAt,
        Instant completedAt
) {
}