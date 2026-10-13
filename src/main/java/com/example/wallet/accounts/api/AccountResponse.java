package com.example.wallet.accounts.api;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID clientId,
        String currency,
        String balance,
        String status,
        Instant createdAt
) {
}