package com.example.wallet.transfers;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Optional<Transfer> findById(UUID id);

    void save(Transfer transfer);
}
