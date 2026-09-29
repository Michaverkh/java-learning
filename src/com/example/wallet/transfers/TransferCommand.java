package com.example.wallet.transfers;

import com.example.wallet.accounts.AccountId;
import com.example.wallet.shared.money.Currency;
import com.example.wallet.shared.money.Money;

import java.math.BigDecimal;

public final record TransferCommand(
        AccountId sourceAccountId,
        AccountId targetAccountId,
        BigDecimal amount,
        Currency currency,
        String description) {
}
