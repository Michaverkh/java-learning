package com.example.exercises.week_3.lambdas.task_4;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class TransferFactory {
    private final Supplier<UUID> idGenerator;
    private final Clock clock;

    public TransferFactory(Supplier<UUID> idGenerator, Clock clock) {
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    Transfer create(CreateTransferCommand command) {
        final UUID id = idGenerator.get();
        final TransferStatus status = TransferStatus.COMPLETED;
        final Instant completedAt = clock.instant();
        final Transfer transfer = new Transfer(id, command.sourceAccountId(), command.targetAccountId(), command.amount(), command.currency(), status, completedAt, completedAt);

        return transfer;
    }
}
