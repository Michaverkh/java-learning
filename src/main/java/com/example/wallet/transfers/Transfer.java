package com.example.wallet.transfers;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transfer(
        UUID id,
        AccountId sourceAccountId,
        AccountId targetAccountId,
        BigDecimal amount,
        Currency currency,
        TransferStatus status,
        Instant createdAt,
        Instant completedAt
) {
}
