package com.example.wallet.ledger;

import com.example.wallet.accounts.AccountId;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public final class LedgerEntryFactory {
    private final Supplier<UUID> idGenerator;
    private final Clock clock;

    public LedgerEntryFactory(Supplier<UUID> idGenerator, Clock clock) {
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * amount передаётся со знаком; operationId связывает записи одной операции.
     */
    public LedgerEntry create(
            AccountId accountId,
            UUID operationId,
            OperationType operationType,
            BigDecimal amount,
            BigDecimal balanceAfter
    ) {
        return new LedgerEntry(
                idGenerator.get(), accountId, operationId, operationType,
                amount, balanceAfter, clock.instant()
        );
    }
}
