package com.example.wallet.transfers;

import com.example.wallet.configuration.TransferProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public final class TransferLimitPolicy {
    private final BigDecimal maxAmount;

    public TransferLimitPolicy(TransferProperties properties) {
        this.maxAmount = properties.maxAmount();
    }

    public void check(BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("transfer amount exceeds configured limit");
        }
    }
}