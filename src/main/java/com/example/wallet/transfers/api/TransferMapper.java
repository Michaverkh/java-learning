package com.example.wallet.transfers.api;

import com.example.wallet.transfers.Transfer;

import java.math.RoundingMode;
import java.util.Objects;

public final class TransferMapper {
    private TransferMapper() {
    }

    public static TransferResponse toResponse(Transfer transfer) {
        Objects.requireNonNull(transfer, "account must not be null");

        return new TransferResponse(
                transfer.id(),
                transfer.sourceAccountId().getValue(),
                transfer.targetAccountId().getValue(),
                transfer.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
                transfer.currency().name(),
                transfer.status().name(),
                transfer.createdAt(),
                transfer.completedAt()
        );
    }
}
