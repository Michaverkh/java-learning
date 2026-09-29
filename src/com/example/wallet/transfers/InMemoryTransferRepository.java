package com.example.wallet.transfers;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public final class InMemoryTransferRepository implements TransferRepository {
    private final Map<UUID, Transfer> transfers = new HashMap<>();

    @Override
    public Optional<Transfer> findById(UUID id) {
        return Optional.ofNullable(transfers.get(id));
    }

    @Override
    public void save(Transfer transfer) {
        Objects.requireNonNull(transfer, "transfer must not be null");
        transfers.put(transfer.id(), transfer);
    }
}
